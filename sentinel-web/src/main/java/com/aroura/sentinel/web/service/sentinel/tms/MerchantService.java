package com.aroura.sentinel.web.service.sentinel.tms;

import com.aroura.sentinel.logistics.dao.tms.MerchantDao;
import com.aroura.sentinel.logistics.model.tms.Merchant;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 商家主数据服务
 *
 * @author sentinel
 */
@Service
public class MerchantService {

    private final MerchantDao merchantDao;

    /** userId → 商家ID 短缓存，避免每个请求都回查 merchant 表 */
    private final Map<Long, CachedId> userIdCache = new ConcurrentHashMap<>();
    /** username → 商家ID，仅供 legacy 会话（不含 userId）回退使用 */
    private final Map<String, CachedId> usernameCache = new ConcurrentHashMap<>();

    /** 缓存秒数；设为 0 关闭缓存（造数/联调阶段用，让验证结果确定可复现） */
    @Value("${sentinel.tenant.merchant-cache-seconds:300}")
    private long merchantCacheSeconds;

    public MerchantService(MerchantDao merchantDao) {
        this.merchantDao = merchantDao;
    }

    public Long save(Map<String, Object> body) {
        Long id = body.get("id") == null ? null : Long.valueOf(String.valueOf(body.get("id")));
        Merchant.MerchantBuilder b = Merchant.builder()
                .merchantCode(String.valueOf(body.get("merchantCode") == null ? "MCH-" + System.currentTimeMillis() : body.get("merchantCode")))
                .merchantName(String.valueOf(body.get("merchantName")))
                .userId(body.get("userId") == null ? null : Long.valueOf(String.valueOf(body.get("userId"))))
                .contactName(body.get("contactName") == null ? null : String.valueOf(body.get("contactName")))
                .contactPhone(body.get("contactPhone") == null ? null : String.valueOf(body.get("contactPhone")))
                .contactEmail(body.get("contactEmail") == null ? null : String.valueOf(body.get("contactEmail")))
                .country(body.get("country") == null ? "CN" : String.valueOf(body.get("country")))
                .status(body.get("status") == null ? 1 : Integer.valueOf(String.valueOf(body.get("status"))));
        if (id != null) {
            b.id(id);
            merchantDao.update(b.build());
            evictCaches();
            return id;
        }
        Long newId = merchantDao.insert(b.build());
        evictCaches();
        return newId;
    }

    public void delete(Long id) {
        merchantDao.delete(id);
        evictCaches();
    }

    public Map<String, Object> detail(Long id) {
        return merchantDao.findById(id);
    }

    public Map<String, Object> list(String keyword, int page, int perPage) {
        return merchantDao.findPage(keyword, page, perPage);
    }

    /** 商家分页；{@code onlyMerchantId} 非空时只返回该商家自己那一行。 */
    public Map<String, Object> list(String keyword, Long onlyMerchantId, int page, int perPage) {
        return merchantDao.findPage(keyword, onlyMerchantId, page, perPage);
    }

    public List<Map<String, Object>> listAll() {
        return merchantDao.listAll();
    }

    /** 商家名录；{@code onlyMerchantId} 非空时只返回该商家自己那一行（MERCHANT 视角）。 */
    public List<Map<String, Object>> listAll(Long onlyMerchantId) {
        return merchantDao.listAll(onlyMerchantId);
    }

    /**
     * 通过登录用户名解析当前商家（MERCHANT 角色权限隔离用）
     */
    public Map<String, Object> findByUsername(String username) {
        if (username == null) {
            return null;
        }
        List<Map<String, Object>> list = merchantDao.findByUsername(username);
        return list.isEmpty() ? null : list.get(0);
    }

    // ------------------------------------------------------------------
    // 租户解析：登录身份 → 商家ID。null 表示「未绑定」，
    // 调用方（TenantScopeResolver）据此判定 MERCHANT 是否应被拒绝。
    // ------------------------------------------------------------------

    /** 按 sentinel_user.id 解析商家ID（走 idx_user_id，比 JOIN sentinel_user 便宜）。 */
    public Long findMerchantIdByUserId(Long userId) {
        if (userId == null) {
            return null;
        }
        CachedId hit = readCache(userIdCache, userId);
        if (hit != null) {
            return hit.value;
        }
        Long id = idOf(merchantDao.findByUserId(userId));
        writeCache(userIdCache, userId, id);
        return id;
    }

    /** 按用户名解析商家ID。仅 legacy 会话（不含 userId）的回退路径。 */
    public Long findMerchantIdByUsername(String username) {
        if (username == null || username.trim().isEmpty()) {
            return null;
        }
        String key = username.trim();
        CachedId hit = readCache(usernameCache, key);
        if (hit != null) {
            return hit.value;
        }
        Long id = idOf(merchantDao.findByUsername(key).stream().findFirst().orElse(null));
        writeCache(usernameCache, key, id);
        return id;
    }

    /** 注册流程补建商家档案用；失败返回 null，由调用方决定是否容忍。 */
    public Long createForNewUser(Long userId, String merchantName) {
        Merchant m = Merchant.builder()
                .merchantCode("MCH-" + userId)
                .merchantName(merchantName == null || merchantName.trim().isEmpty() ? "商家" + userId : merchantName)
                .userId(userId)
                .country("CN")
                .status(1)
                .build();
        return merchantDao.insert(m);
    }

    private static Long idOf(Map<String, Object> row) {
        if (row == null || row.get("id") == null) {
            return null;
        }
        return Long.valueOf(String.valueOf(row.get("id")));
    }

    /** 读缓存；未命中或已过期返回 null。缓存内容本身可以是 null（表示"确认未绑定"）。 */
    private <K> CachedId readCache(Map<K, CachedId> cache, K key) {
        if (merchantCacheSeconds <= 0) {
            return null;
        }
        CachedId c = cache.get(key);
        if (c == null || System.currentTimeMillis() - c.at >= merchantCacheSeconds * 1000L) {
            return null;
        }
        return c;
    }

    private <K> void writeCache(Map<K, CachedId> cache, K key, Long value) {
        if (merchantCacheSeconds <= 0) {
            return;
        }
        cache.put(key, new CachedId(value, System.currentTimeMillis()));
    }

    /** 商家绑定关系变更后调用，避免缓存陈旧。 */
    public void evictCaches() {
        userIdCache.clear();
        usernameCache.clear();
    }

    private static final class CachedId {
        private final Long value;
        private final long at;

        private CachedId(Long value, long at) {
            this.value = value;
            this.at = at;
        }
    }
}
