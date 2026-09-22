package com.aroura.msg.web;

import java.io.IOException;
import java.util.UUID;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * msg-service 侧的请求关联 ID。
 * <p>
 * 这里没有复用 {@code com.aroura.sentinel.ms.web.RequestIdFilter}，因为 msg-service
 * 刻意不依赖 sentinel-ms-web（那会把整套 MVC 鉴权装配带进来）。代价是这段逻辑有两份 ——
 * 头名与 MDC 键必须保持一致，改动时两边都要改。
 * <p>
 * 缺了它，整条通知链路会在最后一跳断掉：logistics 与 agent 的日志有 ID，真正把短信发出去的
 * msg-service 却没有，恰恰是排障最需要的那一段。
 *
 * @author sentinel-ms
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestIdFilter extends OncePerRequestFilter {

    /** 与 sentinel-ms-web 的 RequestIdFilter 保持一致。 */
    public static final String HDR_REQUEST_ID = "X-Request-Id";
    public static final String MDC_KEY = "requestId";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String requestId = request.getHeader(HDR_REQUEST_ID);
        if (requestId == null || requestId.trim().isEmpty()) {
            requestId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        }
        MDC.put(MDC_KEY, requestId);
        response.setHeader(HDR_REQUEST_ID, requestId);
        try {
            chain.doFilter(request, response);
        } finally {
            MDC.remove(MDC_KEY);
        }
    }
}
