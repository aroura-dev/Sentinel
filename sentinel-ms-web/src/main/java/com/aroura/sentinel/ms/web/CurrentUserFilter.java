package com.aroura.sentinel.ms.web;

import com.aroura.sentinel.web.vo.CurrentUserVO;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * 微服务共享：还原网关注入的 {@code X-User-Name / X-User-Role} 身份头，
 * 供业务代码以 request attribute {@code currentUser} 读取（key 与单体 AuthInterceptor 一致，
 * 故控制器的行级过滤代码零改动）。
 *
 * <h3>为什么必须验签</h3>
 * 身份头本身是不可信输入。端口收敛挡住了宿主机直连，但同一 compose 网络内（或被攻陷的任一容器）
 * 仍可构造这两个头冒充任意角色 —— 而 {@code /internal/**} 这类端点上根本没有 @RequireRole。
 * 因此这里不满足于「有头就认」，而是要求头必须携带网关用共享密钥签出的签名。
 *
 * <h3>拒绝策略</h3>
 * <ul>
 *   <li>带了身份头但签名无效/过期 → <b>401</b> 直接挡回。不采用「忽略身份继续放行」——
 *       那会让没有 @RequireRole 的端点（如 /internal/**）在伪造请求下照常执行。</li>
 *   <li>完全没有身份头 → 匿名放行，是否拒绝交给 @RequireRole（默认拒绝）。</li>
 *   <li>{@code /internal/**} → 必须带有效的**服务**签名，与用户身份无关。</li>
 * </ul>
 * <p>
 * 未配置密钥时跳过校验并告警一次 —— 仅用于本地无网关联调，生产必须配置。
 *
 * @author sentinel-ms
 */
public class CurrentUserFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(CurrentUserFilter.class);

    public static final String CURRENT_USER_ATTR = "currentUser";
    public static final String HDR_USER_NAME = "X-User-Name";
    public static final String HDR_USER_ROLE = "X-User-Role";

    private final String secret;
    private final long maxSkewSeconds;
    private volatile boolean warnedNoSecret;

    public CurrentUserFilter(String secret, long maxSkewSeconds) {
        this.secret = secret;
        this.maxSkewSeconds = maxSkewSeconds;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (secret == null || secret.isEmpty()) {
            if (!warnedNoSecret) {
                warnedNoSecret = true;
                log.warn("[CurrentUserFilter] sentinel.internal.secret 未配置，身份头签名校验已跳过。"
                        + "仅可用于本地无网关联调，生产环境必须配置密钥。");
            }
            chain.doFilter(request, response);
            return;
        }

        String path = request.getRequestURI();

        // 内部接口：只认服务签名，与用户身份无关
        if (path != null && path.startsWith("/internal/")) {
            String service = request.getHeader(InternalAuth.HDR_SERVICE);
            if (service == null || !InternalAuth.verify(secret, service, InternalAuth.SERVICE_ROLE,
                    request.getHeader(InternalAuth.HDR_TIMESTAMP),
                    request.getHeader(InternalAuth.HDR_SIGNATURE), maxSkewSeconds)) {
                log.warn("[CurrentUserFilter] 内部接口服务签名校验失败 path={} service={}", path, service);
                reject(response);
                return;
            }
            chain.doFilter(request, response);
            return;
        }

        // 网关把用户名百分号编码后放进头（HTTP 头值按规范是 ISO-8859-1，直接放中文会被改写成乱码）。
        // 签名覆盖的正是这个编码后的值，所以先验签、再解码还原成真实用户名。
        String encodedName = request.getHeader(HDR_USER_NAME);
        if (encodedName != null && !encodedName.isEmpty()) {
            String role = request.getHeader(HDR_USER_ROLE);
            if (!InternalAuth.verify(secret, encodedName, role,
                    request.getHeader(InternalAuth.HDR_TIMESTAMP),
                    request.getHeader(InternalAuth.HDR_SIGNATURE), maxSkewSeconds)) {
                log.warn("[CurrentUserFilter] 身份头签名校验失败 username={} role={} path={}", encodedName, role, path);
                reject(response);
                return;
            }
            CurrentUserVO vo = new CurrentUserVO();
            vo.setUsername(decodeUsername(encodedName));
            vo.setRole(role == null ? "" : role);
            request.setAttribute(CURRENT_USER_ATTR, vo);
        }
        chain.doFilter(request, response);
    }

    /** 还原网关注入时做的百分号编码；解不开就按原值使用，避免因格式问题把用户挡在门外。 */
    private String decodeUsername(String encoded) {
        try {
            return URLDecoder.decode(encoded, StandardCharsets.UTF_8.name());
        } catch (Exception e) {
            log.warn("[CurrentUserFilter] 用户名解码失败，按原值使用 encoded={}", encoded);
            return encoded;
        }
    }

    private void reject(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write("{\"status\":401,\"msg\":\"身份签名校验失败\",\"data\":null}");
    }
}
