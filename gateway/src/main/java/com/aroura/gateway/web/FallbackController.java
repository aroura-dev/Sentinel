package com.aroura.gateway.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 熔断 fallback 端点：路由的 {@code fallbackUri: forward:/gateway-fallback/xxx} 落在这里。
 * <p>
 * {@code forward:} 由网关自带的 ForwardRoutingFilter 派发到本进程的 WebFlux handler，
 * 不再经过 global filters，所以这里**不需要身份头**（也就不必参与网关鉴权）。
 *
 * <h3>为什么手写报文而不复用 BasicResultVO</h3>
 * 网关是 WebFlux 应用，依赖 {@code sentinel-common} 会把 lombok + fastjson2 一并拉进来，
 * 只为一个 VO 不值当。这里手写与之**同形**的 {@code {status,msg,data}}
 * （字段名与 {@code com.aroura.sentinel.common.vo.BasicResultVO} 一致），
 * 与 {@code AuthGlobalFilter} 里已有的手写 401 响应是同一取舍。
 * 改动一侧时记得同步另一侧。
 *
 * @author sentinel-ms
 */
@RestController
public class FallbackController {

    private static final Logger log = LoggerFactory.getLogger(FallbackController.class);

    /** 与 RespStatusEnum.SERVICE_ERROR 对齐 */
    private static final String SERVICE_ERROR = "B0001";

    @RequestMapping("/gateway-fallback/**")
    public ResponseEntity<Map<String, Object>> fallback(ServerWebExchange exchange) {
        String path = exchange.getRequest().getPath().value();
        log.warn("[GatewayFallback] 下游不可用（熔断打开或调用失败），返回降级响应 path={}", path);

        // HTTP 200 + 业务码：本项目约定如此（登录失败同样是 200 + status 码，见 infra/tools/up.sh）。
        // 故这里不返回 503 —— 前端按 status 判成败，HTTP 码只表达"网关这一跳本身成功了"。
        Map<String, Object> body = new LinkedHashMap<>(3);
        body.put("status", SERVICE_ERROR);
        body.put("msg", "服务暂不可用，请稍后重试");
        body.put("data", null);
        return ResponseEntity.ok(body);
    }
}
