package com.aroura.logistics.client;

import java.util.HashMap;
import java.util.Map;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
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

    private final RestTemplate restTemplate;
    private final String agentUrl;

    public AgentClient(RestTemplate restTemplate, @Value("${service.agent-url}") String agentUrl) {
        this.restTemplate = restTemplate;
        this.agentUrl = agentUrl;
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
            String resp = restTemplate.postForObject(agentUrl + "/internal/agent/generate", body, String.class);
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
