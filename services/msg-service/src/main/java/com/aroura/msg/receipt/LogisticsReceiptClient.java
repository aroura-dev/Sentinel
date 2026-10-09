package com.aroura.msg.receipt;

import java.util.HashMap;
import java.util.Map;

import com.aroura.sentinel.ms.web.InternalAuth;
import com.aroura.sentinel.ms.web.RequestIdFilter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * 把投递回执回调给 logistics-service。
 * <p>
 * 这是本服务唯一一条「反向」调用：正常情况下是 logistics 调 msg，而回执需要把
 * 渠道的真实结果送回去。走 thin REST 而不引入对 logistics 领域库的依赖。
 * <p>
 * 目标是 {@code /internal/**}，因此必须带**服务签名**，与 logistics 调 agent 时同一套机制。
 *
 * @author sentinel-ms
 */
@Component
public class LogisticsReceiptClient {

    private static final Logger log = LoggerFactory.getLogger(LogisticsReceiptClient.class);
    /** 本服务在服务间签名中的身份标识。 */
    private static final String SERVICE_NAME = "svc:msg";

    private final RestTemplate restTemplate;
    private final String logisticsUrl;
    private final String internalSecret;

    public LogisticsReceiptClient(RestTemplate restTemplate,
                                  @Value("${service.logistics-url:http://127.0.0.1:8083}") String logisticsUrl,
                                  @Value("${sentinel.internal.secret:}") String internalSecret) {
        this.restTemplate = restTemplate;
        this.logisticsUrl = logisticsUrl;
        this.internalSecret = internalSecret;
    }

    /**
     * 上报一条回执。
     * <p>
     * 失败只记日志、不抛出：回执丢失会让该记录停在 SENT（即当前行为），
     * 而抛出异常会打断发送链路 —— 拿主线稳定换一条辅助信息不值得。
     */
    public void report(String bizId, boolean accepted, String detail) {
        Map<String, Object> body = new HashMap<>(4);
        body.put("bizId", bizId);
        body.put("accepted", accepted);
        body.put("detail", detail);
        try {
            restTemplate.postForObject(logisticsUrl + "/internal/notify/receipt",
                    new HttpEntity<>(body, headers()), String.class);
        } catch (Exception e) {
            log.warn("[Receipt] 回执上报失败 bizId={} accepted={} err={}", bizId, accepted, e.getMessage());
        }
    }

    private HttpHeaders headers() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String requestId = MDC.get(RequestIdFilter.MDC_KEY);
        if (requestId != null && !requestId.isEmpty()) {
            headers.set(RequestIdFilter.HDR_REQUEST_ID, requestId);
        }
        if (internalSecret != null && !internalSecret.isEmpty()) {
            long ts = System.currentTimeMillis() / 1000L;
            headers.set(InternalAuth.HDR_SERVICE, SERVICE_NAME);
            headers.set(InternalAuth.HDR_TIMESTAMP, String.valueOf(ts));
            headers.set(InternalAuth.HDR_SIGNATURE,
                    InternalAuth.sign(internalSecret, SERVICE_NAME, InternalAuth.SERVICE_ROLE, ts));
        }
        return headers;
    }
}
