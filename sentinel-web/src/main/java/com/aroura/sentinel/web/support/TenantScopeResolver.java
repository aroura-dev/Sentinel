package com.aroura.sentinel.web.support;

import com.aroura.sentinel.web.config.AuthInterceptor;
import com.aroura.sentinel.web.exception.CommonException;
import com.aroura.sentinel.web.service.sentinel.tms.MerchantService;
import com.aroura.sentinel.web.vo.CurrentUserVO;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;

/**
 * 统一的租户（商家）作用域解析，取代此前散落在 6 个 Controller 里复制粘贴的
 * {@code resolveMerchantScope} / {@code resolveMerchant} / {@code enforceMerchantScope}。
 *
 * <h3>语义总纲</h3>
 * <ul>
 *   <li><b>{@code scope == null} 恒等于「不限制」</b>，永不等价于「限制到空集」。
 *       无请求上下文的调用方（{@code CompletableFuture.runAsync} 后台线程、sentinel-cron）
 *       解析结果就是「不限制」，失败方向是维持现状而不是数据消失。</li>
 *   <li><b>MERCHANT</b> 恒为自身商家；传入的 {@code merchantId} 一律<b>静默丢弃</b>，绝不报错
 *       —— 前端 OrderList 会把含 merchantId 的筛选持久化进 localStorage，
 *       共用浏览器下商家登录会重放他人 merchantId，报错会让页面直接不可用。</li>
 *   <li><b>MERCHANT 但未绑定商家档案</b> → 直接拒绝。这是改造前最严重的漏洞成因：
 *       此前 6 份 helper 中多数在查不到商家时返回 {@code null}（=不限制），
 *       等于「没有商家档案的商家账号能看到全平台数据」。</li>
 *   <li>平台角色（ADMIN / OPERATOR / FINANCE / CUSTOMER_SERVICE）不限制，保留
 *       「代操作 / 代查看指定商家」能力。</li>
 * </ul>
 *
 * <h3>为什么不做成 ThreadLocal</h3>
 * 本仓库没有作用于 DAO 层的 AOP，事务边界是在部分业务方法上逐个标注的（见各 service 的
 * {@code @Transactional}），并非全覆盖；Tomcat 线程复用下一旦某条路径漏了清理，
 * 上一个租户的作用域会静默套用到下一个请求 —— 这是隔离方案里最严重的失效模式。
 * 因此作用域始终以显式参数下传，DAO 层谓词只是机制、不是安全边界，
 * <b>安全判定必须在知道角色的这一层完成</b>。
 *
 * @author sentinel
 */
@Component
public class TenantScopeResolver {

    private static final String ROLE_MERCHANT = "MERCHANT";

    private final MerchantService merchantService;

    public TenantScopeResolver(MerchantService merchantService) {
        this.merchantService = merchantService;
    }

    /**
     * 当前请求的有效租户作用域。
     *
     * @return {@code null} = 不限制（平台角色 / 后台线程）；
     *         非 null = 限定到该商家ID
     * @throws CommonException 当前是 MERCHANT 角色但未绑定商家档案
     */
    public Long currentScope() {
        CurrentUserVO user = currentUser();
        if (user == null || !isMerchant(user)) {
            return null;
        }
        return requireOwnMerchantId(user);
    }

    /** 不抛异常的版本：MERCHANT 未绑定档案时同样返回 null，仅供 /auth/me 之类展示用途。 */
    public Long currentScopeOrNull() {
        CurrentUserVO user = currentUser();
        if (user == null || !isMerchant(user)) {
            return null;
        }
        return resolveMerchantId(user);
    }

    /** 是否平台级身份（已登录且非 MERCHANT）。无请求上下文时为 false。 */
    public boolean isPlatformStaff() {
        CurrentUserVO user = currentUser();
        return user != null && !isMerchant(user);
    }

    /**
     * 当前请求是否为 MERCHANT 角色。无请求上下文（后台线程）时为 false。
     * <p>
     * 需要区分「MERCHANT 未绑定商家」与「平台角色」时用它 ——
     * 这两者在 {@link #currentScopeOrNull()} 下都返回 null，
     * 若不加区分地当成「不限制」，未绑定商家的账号反而能拿到全量数据。
     */
    public boolean isMerchant() {
        CurrentUserVO user = currentUser();
        return user != null && isMerchant(user);
    }

    /**
     * 归一化入参 merchantId：MERCHANT 一律替换为自身作用域（静默，不报错）；
     * 其他角色原样透传（null 即不限制）。
     */
    public Long normalizeRequested(Long requested) {
        CurrentUserVO user = currentUser();
        if (user == null || !isMerchant(user)) {
            return requested;
        }
        return requireOwnMerchantId(user);
    }

    /**
     * 归属断言：MERCHANT 访问他人商家数据时拒绝。用于单行读 / 按 id 写（"取回后断言"手法）。
     * <p>
     * 之所以用断言而不是把 scope 拼进 {@code UPDATE ... WHERE id=? AND merchant_id=?}：
     * 后者影响 0 行时分不清「不存在」和「不是你的」，会返回一个误导性的成功。
     *
     * @param actualMerchantId 目标数据实际所属商家ID，允许为 null（视为无权）
     * @param objectDesc       用于提示语的对象名，如"商品""订单"
     */
    public void assertAccessible(Long actualMerchantId, String objectDesc) {
        CurrentUserVO user = currentUser();
        if (user == null || !isMerchant(user)) {
            return;
        }
        Long own = requireOwnMerchantId(user);
        if (actualMerchantId == null || !own.equals(actualMerchantId)) {
            throw new CommonException("无权访问其他商家的" + objectDesc);
        }
    }

    /** 当前登录用户；无请求上下文（后台线程）时返回 null。 */
    public CurrentUserVO currentUser() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) {
            return null;
        }
        HttpServletRequest request = attrs.getRequest();
        Object attr = request.getAttribute(AuthInterceptor.CURRENT_USER_ATTR);
        return attr instanceof CurrentUserVO ? (CurrentUserVO) attr : null;
    }

    private static boolean isMerchant(CurrentUserVO user) {
        return ROLE_MERCHANT.equals(user.getRole());
    }

    /** MERCHANT 未绑定商家档案时拒绝，绝不退化成「不限制」。 */
    private Long requireOwnMerchantId(CurrentUserVO user) {
        Long merchantId = resolveMerchantId(user);
        if (merchantId == null) {
            throw new CommonException("当前账号未绑定商家，请联系管理员");
        }
        return merchantId;
    }

    /**
     * 以数据库为准解析商家归属，不直接信任会话里的 merchantId —— 这样管理员事后补绑商家时，
     * 存量会话无需重新登录即可生效，也不会因会话陈旧而放行错误的租户。
     * 实际的查库开销由 {@link MerchantService} 的短缓存兜住。
     */
    private Long resolveMerchantId(CurrentUserVO user) {
        if (user.getUserId() != null) {
            return merchantService.findMerchantIdByUserId(user.getUserId());
        }
        // legacy 会话不含 userId，按用户名回退（行为与改造前一致）
        return merchantService.findMerchantIdByUsername(user.getUsername());
    }
}
