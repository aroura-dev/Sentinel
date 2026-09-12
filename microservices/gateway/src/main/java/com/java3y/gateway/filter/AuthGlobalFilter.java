package com.java3y.gateway.filter;

import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

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
 * 命中 → 注入 X-User-Name / X-User-Role 头透传给业务服务（业务侧 austin-ms-web 还原 currentUser），
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
    private static final String LOGIN_PATH = "/api/auth/login";
    private static final String SMS_PUBLIC_PREFIX = "/api/auth/sms/";
    private static final Pattern UNAUTHED_API = Pattern.compile("^/api/(actuator|swagger).*");

    private final ReactiveStringRedisTemplate redis;

    public AuthGlobalFilter(ReactiveStringRedisTemplate redis) {
        this.redis = redis;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        // 公开/放行路径不校验
        if (LOGIN_PATH.equals(path)
                || path.startsWith(SMS_PUBLIC_PREFIX)
                || UNAUTHED_API.matcher(path).matches()
                || "OPTIONS".equalsIgnoreCase(request.getMethodValue())) {
            return chain.filter(exchange);
        }
        // 非受管前缀（内部/公开接口）直通，不强制登录
        if (!path.startsWith("/api/")) {
            return chain.filter(exchange);
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
                    ServerHttpRequest mutated = request.mutate()
                            .header(HDR_USER_NAME, session.username)
                            .header(HDR_USER_ROLE, session.role)
                            .build();
                    return chain.filter(exchange.mutate().request(mutated).build());
                })
                .switchIfEmpty(unauthorized(exchange, "未登录或登录已过期"));
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
