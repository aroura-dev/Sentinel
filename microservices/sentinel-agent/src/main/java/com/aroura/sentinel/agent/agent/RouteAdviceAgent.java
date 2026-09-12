package com.aroura.sentinel.agent.agent;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.aroura.sentinel.agent.assistant.RouteAdviceAssistant;
import com.aroura.sentinel.agent.config.AgentTelemetry;
import com.aroura.sentinel.agent.dto.RouteAdviceResult;
import com.aroura.sentinel.agent.service.AgentCallLogService;
import com.aroura.sentinel.agent.tool.ChannelQuoteTool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Agent 7：渠道推荐 Agent（RouteAdviceAgent）
 * <p>
 * 职责：目的地 + 重量 → 基于真实价卡推荐最优物流渠道。
 * LLM 不可用时降级为「按真实价卡价格最低推荐」。
 *
 * @author sentinel
 */
@Component
public class RouteAdviceAgent {

    private static final Logger log = LoggerFactory.getLogger(RouteAdviceAgent.class);
    private static final String AGENT_NAME = "RouteAdviceAgent";

    @Autowired
    private RouteAdviceAssistant routeAdviceAssistant;
    @Autowired
    private ChannelQuoteTool channelQuoteTool;
    @Autowired
    private AgentCallLogService agentCallLogService;
    @Autowired
    private AgentTelemetry agentTelemetry;

    public JSONObject advise(String destCountry, String weightKg, String traceId) {
        long start = System.currentTimeMillis();
        try {
            agentTelemetry.begin();
            RouteAdviceResult result = routeAdviceAssistant.advise(destCountry, weightKg);
            AgentTelemetry.Telemetry telemetry = agentTelemetry.capture();
            long latency = System.currentTimeMillis() - start;
            JSONObject out = result == null ? fallback(destCountry, weightKg)
                    : JSON.parseObject(JSON.toJSONString(result));
            JSONObject input = new JSONObject();
            input.put("destCountry", destCountry);
            input.put("weightKg", weightKg);
            agentCallLogService.record(AGENT_NAME, input, out, telemetry.toolsCalled(),
                    telemetry.getTotalTokens() > 0 ? telemetry.getTotalTokens() : null, latency, "success", traceId);
            return out;
        } catch (Exception e) {
            agentTelemetry.capture();
            long latency = System.currentTimeMillis() - start;
            log.error("[{}] LLM 调用失败，降级为真实价卡推荐", AGENT_NAME, e);
            JSONObject fallback = fallback(destCountry, weightKg);
            agentCallLogService.record(AGENT_NAME, null, fallback, null, null, latency, "degraded", traceId);
            return fallback;
        }
    }

    /**
     * 降级：直接调真实价卡工具，返回价格最低渠道
     */
    private JSONObject fallback(String destCountry, String weightKg) {
        JSONObject result = new JSONObject();
        try {
            JSONArray arr = JSON.parseArray(channelQuoteTool.quoteChannels(destCountry, weightKg));
            if (arr.isEmpty()) {
                result.put("reason", "目的地无可用渠道或价卡");
                return result;
            }
            JSONObject best = arr.getJSONObject(0);
            result.put("recommendedChannelId", best.getLong("channelId"));
            result.put("channelCode", best.getString("channelCode"));
            result.put("channelName", best.getString("channelName"));
            result.put("carrierName", best.getString("carrierName"));
            result.put("type", best.getString("type"));
            result.put("freight", best.getBigDecimal("freight"));
            result.put("currency", best.getString("currency"));
            result.put("transitDaysMax", best.getInteger("transitDaysMax"));
            result.put("reason", "按真实价卡价格最低推荐（降级模式）");
        } catch (Exception e) {
            result.put("reason", "渠道推荐失败");
        }
        return result;
    }
}
