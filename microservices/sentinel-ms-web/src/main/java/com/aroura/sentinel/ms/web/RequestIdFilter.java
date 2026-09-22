package com.aroura.sentinel.ms.web;

import java.io.IOException;
import java.util.UUID;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * 请求关联 ID：让「一个请求在多个服务里产生的日志」能被串起来。
 * <p>
 * 此前日志里没有任何关联标识 —— 排障时只能靠时间戳猜，跨服务的调用（网关 → logistics →
 * agent → msg）完全无法还原。这里给每个请求分配一个 ID 并放进 MDC，日志格式把它打出来。
 *
 * <h3>ID 的来源</h3>
 * 优先用上游传来的 {@code X-Request-Id}：网关会在入口生成，下游服务透传，
 * 因此整条链路共享同一个 ID。直接访问某个服务（无网关）时自行生成。
 *
 * <h3>与业务 traceId 的区别</h3>
 * 业务 traceId（{@code orderNo|node|lang|role}）用于把三库里的记录关联起来，是**数据**层面的；
 * 这里的 requestId 是**请求**层面的，一次请求可能产生零到多条业务记录。两者互补，不互相替代。
 *
 * @author sentinel-ms
 */
public class RequestIdFilter extends OncePerRequestFilter {

    /** 上游传递与下游回传共用的头名。 */
    public static final String HDR_REQUEST_ID = "X-Request-Id";
    /** 日志 MDC 中的键名，与 logging.pattern 里的 %X{requestId} 对应。 */
    public static final String MDC_KEY = "requestId";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String requestId = request.getHeader(HDR_REQUEST_ID);
        if (requestId == null || requestId.trim().isEmpty()) {
            requestId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        }
        MDC.put(MDC_KEY, requestId);
        // 回传给出调用方，便于把前端/压测工具看到的一次失败与后端日志对上
        response.setHeader(HDR_REQUEST_ID, requestId);
        try {
            chain.doFilter(request, response);
        } finally {
            // 线程会被复用，不清会串到下一个请求的日志上
            MDC.remove(MDC_KEY);
        }
    }
}
