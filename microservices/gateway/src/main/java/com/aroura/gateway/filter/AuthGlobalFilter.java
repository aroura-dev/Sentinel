package com.aroura.gateway.filter;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.regex.Pattern;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Mono;

/**
 * 统一认证网关过滤器：/api/**（白名单除外）校验 Bearer 会话 token。
 * <p>
 * 与单体 AuthService 共用 Redis key {@code sentinel:token:{uuid}}（值 username:role）：
 * 命中 → 注入 X-User-Name / X-User-Role 头透传给业务服务（业务侧 sentinel-ms-web 还原 currentUser），
 * 原始 Authorization 头原样透传（供 auth /me、/logout 使用）；未命中回 401。
 * 角色校验（@RequireRole）仍由各业务服务做，避免网关维护 URL→角色映射造成双份漂移。
 *
 * @author sentinel-ms
 */
@Component
public class AuthGlobalFilter implements GlobalFilter, Ordered {

    public static final String TOKEN_PREFIX = "sentinel:token:";
    private static final String HDR_USER_NAME = "X-User-Name";
    private static final String HDR_USER_ROLE = "X-User-Role";
    private static final String HDR_AUTH_TIMESTAMP = "X-Auth-Timestamp";
    private static final String HDR_AUTH_SIGNATURE = "X-Auth-Signature";
    private static final String HDR_SERVICE_NAME = "X-Service-Name";
    private static final String LOGIN_PATH = "/api/auth/login";
    private static final String SMS_PUBLIC_PREFIX = "/api/auth/sms/";
    private static final Pattern UNAUTHED_API = Pattern.compile("^/api/(actuator|swagger).*");

    /** 与各业务服务共享的身份签名密钥；为空则只注入身份头、不签名（下游会跳过校验）。 */
    @Value("${sentinel.internal.secret:}")
    private String internalSecret;

    private final ReactiveStringRedisTemplate redis;

    public AuthGlobalFilter(ReactiveStringRedisTemplate redis) {
        this.redis = redis;
    }

    /**
     * 计算身份签名，附在 X-Auth-Timestamp / X-Auth-Signature 上。
     * <p>
     * <b>必须与 {@code com.aroura.sentinel.ms.web.InternalAuth#sign} 逐字节一致</b>：
     * 网关是 WebFlux 应用，无法依赖 servlet 模块 sentinel-ms-web，故此处内联一份等价实现。
     * 改任一侧都要同步另一侧，否则下游会把所有请求判为签名无效并返回 401。
     */
    private String sign(String principal, String role, long timestampSeconds) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(internalSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            String canonical = principal + "\n" + (role == null ? "" : role) + "\n" + timestampSeconds;
            return Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(mac.doFinal(canonical.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("服务间签名计算失败", e);
        }
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        // 公开/放行路径不校验。
        // 必须剥掉客户端自带的身份头：网关不做认证时若原样透传，客户端就能把
        // X-User-Name/X-User-Role 直接塞进公开接口；加了验签后更糟 —— 伪造头会被下游
        // 判为签名无效，连登录都返回 401。客户端只有一个来源能产生合法身份头：本网关。
        if (LOGIN_PATH.equals(path)
                || path.startsWith(SMS_PUBLIC_PREFIX)
                || UNAUTHED_API.matcher(path).matches()
                || "OPTIONS".equalsIgnoreCase(request.getMethodValue())) {
            return chain.filter(exchange.mutate().request(stripIdentity(request)).build());
        }
        // 非受管前缀（内部/公开接口）直通，不强制登录；同样剥掉身份头
        if (!path.startsWith("/api/")) {
            return chain.filter(exchange.mutate().request(stripIdentity(request)).build());
        }

        String authorization = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return unauthorized(exchange, "未登录");
        }
        String token = authorization.substring(7);

        return redis.opsForValue().get(TOKEN_PREFIX + token)
                .flatMap(value -> {
                    if (value == null) {
                        return unauthorized(exchange, "未登录或登录已过期");
                    }
                    Session session = Session.parse(value);
                    if (session == null) {
                        return unauthorized(exchange, "会话格式异常");
                    }
                    // 用户名可能含中文，而 HTTP 头值按规范是 ISO-8859-1：直接放非 ASCII 会在
                    // 网关(Reactor Netty)编码、下游(Tomcat)解码之间被改写成乱码，
                    // 既让签名对不上，也让依赖用户名的行级隔离（merchant.owner_username）失配。
                    // 故一律百分号编码后再上头，头值恒为 ASCII；下游解码还原。
                    String encodedName = URLEncoder.encode(session.username, StandardCharsets.UTF_8);
                    ServerHttpRequest.Builder builder = request.mutate()
                            .header(HDR_USER_NAME, encodedName)
                            .header(HDR_USER_ROLE, session.role);
                    if (internalSecret != null && !internalSecret.isEmpty()) {
                        // 下游凭此确认身份头确实出自网关，而非同网络内伪造。
                        // 签的是**编码后**的头值，与下游校验时读到的字节一致。
                        long ts = System.currentTimeMillis() / 1000L;
                        builder.header(HDR_AUTH_TIMESTAMP, String.valueOf(ts))
                                .header(HDR_AUTH_SIGNATURE, sign(encodedName, session.role, ts));
                    }
                    return chain.filter(exchange.mutate().request(builder.build()).build());
                })
                .switchIfEmpty(unauthorized(exchange, "未登录或登录已过期"));
    }

    /** 移除一切与身份相关的头。客户端不得自带 —— 只有本网关能产生合法的身份头。 */
    private ServerHttpRequest stripIdentity(ServerHttpRequest request) {
        return request.mutate().headers(h -> {
            h.remove(HDR_USER_NAME);
            h.remove(HDR_USER_ROLE);
            h.remove(HDR_AUTH_TIMESTAMP);
            h.remove(HDR_AUTH_SIGNATURE);
            h.remove(HDR_SERVICE_NAME);
        }).build();
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange, String msg) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        byte[] bytes = ("{\"status\":401,\"msg\":\"" + msg + "\",\"data\":null}").getBytes(StandardCharsets.UTF_8);
        DataBuffer buffer = response.bufferFactory().wrap(bytes);
        return response.writeWith(Mono.just(buffer));
    }

    /** 会话值 username:role（角色为末段大写） */
    private static final class Session {
        final String username;
        final String role;

        Session(String username, String role) {
            this.username = username;
            this.role = role;
        }

        static Session parse(String value) {
            if (value == null) {
                return null;
            }
            int idx = value.lastIndexOf(':');
            if (idx <= 0 || idx >= value.length() - 1) {
                return null;
            }
            return new Session(value.substring(0, idx), value.substring(idx + 1));
        }
    }
}
