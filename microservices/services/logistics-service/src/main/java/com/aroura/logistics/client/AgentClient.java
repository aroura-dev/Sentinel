package com.aroura.logistics.client;

import java.util.HashMap;
import java.util.Map;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
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
 * agent-service 客户端：请求 AI 生成通知文案。
 * agent 侧会写 agent_call_log；本服务不可用时降级返回默认文案（不让主链路中断）。
 *
 * @author sentinel-ms
 */
@Component
public class AgentClient {

    private static final Logger log = LoggerFactory.getLogger(AgentClient.class);

    /** 本服务在服务间签名中的身份标识。 */
    private static final String SERVICE_NAME = "svc:logistics";

    private final RestTemplate restTemplate;
    private final String agentUrl;
    private final String internalSecret;

    public AgentClient(RestTemplate restTemplate,
                       @Value("${service.agent-url}") String agentUrl,
                       @Value("${sentinel.internal.secret:}") String internalSecret) {
        this.restTemplate = restTemplate;
        this.agentUrl = agentUrl;
        this.internalSecret = internalSecret;
    }

    /**
     * 内部接口要求服务签名（见 CurrentUserFilter）：agent-service 校验
     * X-Service-Name / X-Auth-Timestamp / X-Auth-Signature，缺失或错误一律 401。
     * 未配置密钥时不带签名 —— 仅供本地无鉴权联调。
     */
    private HttpHeaders internalHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        // 透传请求关联 ID，否则链路在跨服务这一跳就断了，下游日志无从关联
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

    /**
     * 生成买家通知文案
     *
     * @return 文案；失败/降级时返回空串（由调用方兜底默认文案）
     */
    public String generate(String orderNo, String node, String language, String productInfo, String traceId) {
        Map<String, Object> body = new HashMap<>(6);
        body.put("orderNo", orderNo);
        body.put("node", node);
        body.put("language", language);
        body.put("productInfo", productInfo);
        body.put("traceId", traceId);
        try {
            String resp = restTemplate.postForObject(agentUrl + "/internal/agent/generate",
                    new HttpEntity<>(body, internalHeaders()), String.class);
            if (resp == null) {
                return "";
            }
            JSONObject json = JSON.parseObject(resp);
            JSONObject data = json == null ? null : json.getJSONObject("data");
            String content = data == null ? null : data.getString("content");
            return content == null ? "" : content;
        } catch (Exception e) {
            log.warn("[AgentClient] 生成文案失败，降级默认 orderNo={} node={} err={}", orderNo, node, e.getMessage());
            return "";
        }
    }
}
