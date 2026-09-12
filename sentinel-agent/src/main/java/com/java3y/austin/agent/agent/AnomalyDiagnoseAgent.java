package com.java3y.austin.agent.agent;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.java3y.austin.agent.assistant.AnomalyDiagnoseAssistant;
import com.java3y.austin.agent.config.AgentTelemetry;
import com.java3y.austin.agent.dto.AnomalyDiagnosis;
import com.java3y.austin.agent.service.AgentCallLogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Agent 2：异常诊断 Agent（AnomalyDiagnoseAgent）
 * <p>
 * 职责：物流异常时自动诊断原因、生成解释、给出处理建议（结合异常知识库 RAG）。
 * 输出：JSON {reason, suggestion, priority}
 *
 * @author sentinel
 */
@Component
public class AnomalyDiagnoseAgent {

    private static final Logger log = LoggerFactory.getLogger(AnomalyDiagnoseAgent.class);
    private static final String AGENT_NAME = "AnomalyDiagnoseAgent";

    @Autowired
    private AnomalyDiagnoseAssistant anomalyDiagnoseAssistant;
    @Autowired
    private AgentCallLogService agentCallLogService;
    @Autowired
    private AgentTelemetry agentTelemetry;

    /**
     * 诊断异常
     *
     * @param anomalyType 异常类型
     * @param statusCode  原始状态码
     * @param orderInfo   订单信息
     * @param traceId     链路 ID
     * @return JSON {reason, suggestion, priority}
     */
    public JSONObject diagnose(String anomalyType, String statusCode, String orderInfo, String traceId) {
        long start = System.currentTimeMillis();
        try {
            agentTelemetry.begin();
            AnomalyDiagnosis diagnosis = anomalyDiagnoseAssistant.diagnose(anomalyType, statusCode, orderInfo);
            AgentTelemetry.Telemetry telemetry = agentTelemetry.capture();
            long latency = System.currentTimeMillis() - start;
            JSONObject result = diagnosis == null ? null : JSON.parseObject(JSON.toJSONString(diagnosis));
            if (result == null) {
                result = fallback();
            }
            JSONObject input = new JSONObject();
            input.put("anomalyType", anomalyType);
            input.put("statusCode", statusCode);
            input.put("orderInfo", orderInfo);
            agentCallLogService.record(AGENT_NAME, input, result, telemetry.toolsCalled(),
                    telemetry.getTotalTokens() > 0 ? telemetry.getTotalTokens() : null, latency, "success", traceId);
            return result;
        } catch (Exception e) {
            agentTelemetry.capture();
            long latency = System.currentTimeMillis() - start;
            log.error("[{}] LLM 调用失败，降级到默认诊断", AGENT_NAME, e);
            JSONObject fallback = fallback();
            agentCallLogService.record(AGENT_NAME, null, fallback, null, null, latency, "degraded", traceId);
            return fallback;
        }
    }

    private JSONObject fallback() {
        JSONObject fallback = new JSONObject();
        fallback.put("reason", "异常原因待人工确认");
        fallback.put("suggestion", "请联系物流商核实");
        fallback.put("priority", "P1");
        return fallback;
    }
}
