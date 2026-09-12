package com.aroura.sentinel.agent.agent;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.aroura.sentinel.agent.assistant.EtaPredictAssistant;
import com.aroura.sentinel.agent.config.AgentTelemetry;
import com.aroura.sentinel.agent.dto.EtaPredictResult;
import com.aroura.sentinel.agent.service.AgentCallLogService;
import com.aroura.sentinel.agent.tool.EtaPredictTool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Agent 8：时效预测 Agent（EtaPredictAgent）
 * <p>
 * 职责：订单号 → 估算剩余送达天数（渠道承诺时效 + 当前进度）。
 * LLM 不可用时直接返回工具计算结果。
 *
 * @author sentinel
 */
@Component
public class EtaPredictAgent {

    private static final Logger log = LoggerFactory.getLogger(EtaPredictAgent.class);
    private static final String AGENT_NAME = "EtaPredictAgent";

    @Autowired
    private EtaPredictAssistant etaPredictAssistant;
    @Autowired
    private EtaPredictTool etaPredictTool;
    @Autowired
    private AgentCallLogService agentCallLogService;
    @Autowired
    private AgentTelemetry agentTelemetry;

    public JSONObject predict(String orderNo, String traceId) {
        long start = System.currentTimeMillis();
        try {
            agentTelemetry.begin();
            EtaPredictResult result = etaPredictAssistant.predict(orderNo);
            AgentTelemetry.Telemetry telemetry = agentTelemetry.capture();
            long latency = System.currentTimeMillis() - start;
            JSONObject out = result == null ? fallback(orderNo)
                    : JSON.parseObject(JSON.toJSONString(result));
            JSONObject input = new JSONObject();
            input.put("orderNo", orderNo);
            agentCallLogService.record(AGENT_NAME, input, out, telemetry.toolsCalled(),
                    telemetry.getTotalTokens() > 0 ? telemetry.getTotalTokens() : null, latency, "success", traceId);
            return out;
        } catch (Exception e) {
            agentTelemetry.capture();
            long latency = System.currentTimeMillis() - start;
            log.error("[{}] LLM 调用失败，降级为规则预测", AGENT_NAME, e);
            JSONObject fallback = fallback(orderNo);
            agentCallLogService.record(AGENT_NAME, null, fallback, null, null, latency, "degraded", traceId);
            return fallback;
        }
    }

    private JSONObject fallback(String orderNo) {
        try {
            return JSON.parseObject(etaPredictTool.predictEta(orderNo));
        } catch (Exception e) {
            JSONObject result = new JSONObject();
            result.put("orderNo", orderNo);
            result.put("reason", "时效预测失败");
            return result;
        }
    }
}
