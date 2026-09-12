package com.java3y.austin.agent.agent;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.java3y.austin.agent.assistant.CsRouteAssistant;
import com.java3y.austin.agent.config.AgentTelemetry;
import com.java3y.austin.agent.dto.CsRouteResult;
import com.java3y.austin.agent.service.AgentCallLogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Agent 6：客服路由 Agent（CsRouteAgent）
 * <p>
 * 职责：买家咨询路由——识别咨询意图，决定 AI 自动回复还是转人工。
 * 输出：JSON {intent, route, reply}
 *
 * @author sentinel
 */
@Component
public class CsRouteAgent {

    private static final Logger log = LoggerFactory.getLogger(CsRouteAgent.class);
    private static final String AGENT_NAME = "CsRouteAgent";

    @Autowired
    private CsRouteAssistant csRouteAssistant;
    @Autowired
    private AgentCallLogService agentCallLogService;
    @Autowired
    private AgentTelemetry agentTelemetry;

    /**
     * 路由买家咨询
     *
     * @param message 买家咨询文本
     * @param buyerId 买家 ID
     * @param traceId 链路 ID
     * @return JSON {intent, route, reply}
     */
    public JSONObject route(String message, String buyerId, String traceId) {
        long start = System.currentTimeMillis();
        try {
            agentTelemetry.begin();
            CsRouteResult csResult = csRouteAssistant.route(message, buyerId);
            AgentTelemetry.Telemetry telemetry = agentTelemetry.capture();
            long latency = System.currentTimeMillis() - start;
            JSONObject result = csResult == null ? null : JSON.parseObject(JSON.toJSONString(csResult));
            if (result == null) {
                result = fallback();
            }
            JSONObject input = new JSONObject();
            input.put("message", message);
            input.put("buyerId", buyerId);
            agentCallLogService.record(AGENT_NAME, input, result, telemetry.toolsCalled(),
                    telemetry.getTotalTokens() > 0 ? telemetry.getTotalTokens() : null, latency, "success", traceId);
            return result;
        } catch (Exception e) {
            agentTelemetry.capture();
            long latency = System.currentTimeMillis() - start;
            log.error("[{}] LLM 调用失败，降级到转人工", AGENT_NAME, e);
            JSONObject fallback = fallback();
            agentCallLogService.record(AGENT_NAME, null, fallback, null, null, latency, "degraded", traceId);
            return fallback;
        }
    }

    private JSONObject fallback() {
        JSONObject fallback = new JSONObject();
        fallback.put("intent", "other");
        fallback.put("route", "human");
        fallback.put("reply", "您的咨询已转接人工客服，请稍候。");
        return fallback;
    }
}
