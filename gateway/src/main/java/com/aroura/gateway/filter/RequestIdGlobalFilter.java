package com.aroura.gateway.filter;

import java.util.UUID;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Mono;

/**
 * 请求关联 ID 的入口：生成并向下游透传 {@code X-Request-Id}。
 * <p>
 * 网关是整条链路的唯一入口，在这里分配 ID 才能保证「一次请求 = 一个 ID」。
 * 下游的 {@code RequestIdFilter} 读这个头放进 MDC，日志格式再把它打出来，
 * 于是网关 → logistics → agent → msg 的日志可以按同一个 ID 捞出来。
 *
 * <h3>为什么排在认证之前</h3>
 * 认证失败（401）时下游根本不会收到请求，但网关自己要记日志；排在前面能保证
 * 无论请求是否被放行，都会带上 ID 回传。故 order 取 {@link Ordered#HIGHEST_PRECEDENCE}，
 * 并让 {@link AuthGlobalFilter} 退后一位以免两者并列。
 *
 * <h3>网关自身的日志为什么不带 ID</h3>
 * 本过滤器跑在 Reactor Netty 上，线程会在链路中切换，而 MDC 是 ThreadLocal 的 ——
 * 在这里 put 进 MDC，后续日志很可能落在另一个线程上而丢失。要做到网关日志也带 ID
 * 需要完整的 Reactor 上下文传播（Micrometer Tracing 在 P1-3 里做）。
 * 当前的价值在于**下游**：那里是业务日志所在，也是排障时真正要看的地方。
 *
 * @author sentinel-ms
 */
@Component
public class RequestIdGlobalFilter implements GlobalFilter, Ordered {

    public static final String HDR_REQUEST_ID = "X-Request-Id";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String requestId = exchange.getRequest().getHeaders().getFirst(HDR_REQUEST_ID);
        if (requestId == null || requestId.trim().isEmpty()) {
            requestId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        }
        // 回传给调用方：前端或压测工具拿到一次失败时，可以凭这个 ID 直接捞后端日志
        exchange.getResponse().getHeaders().set(HDR_REQUEST_ID, requestId);
        ServerHttpRequest mutated = exchange.getRequest().mutate()
                .header(HDR_REQUEST_ID, requestId)
                .build();
        return chain.filter(exchange.mutate().request(mutated).build());
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
