package com.aroura.sentinel.agent.agent;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.aroura.sentinel.agent.assistant.WorkorderAssistant;
import com.aroura.sentinel.agent.config.AgentTelemetry;
import com.aroura.sentinel.agent.dto.WorkorderResult;
import com.aroura.sentinel.agent.service.AgentCallLogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Agent 5：工单处理 Agent（WorkorderAgent）
 * <p>
 * 职责：异常工单自动分类、优先级判定、生成处理 SOP。
 * 输出：JSON {type, level, sop}
 *
 * @author sentinel
 */
@Component
public class WorkorderAgent {

    private static final Logger log = LoggerFactory.getLogger(WorkorderAgent.class);
    private static final String AGENT_NAME = "WorkorderAgent";

    @Autowired
    private WorkorderAssistant workorderAssistant;
    @Autowired
    private AgentCallLogService agentCallLogService;
    @Autowired
    private AgentTelemetry agentTelemetry;

    /**
     * 处理工单
     *
     * @param anomalyDesc 异常描述
     * @param orderInfo   订单信息
     * @param traceId     链路 ID
     * @return JSON {type, level, sop}
     */
    public JSONObject process(String anomalyDesc, String orderInfo, String traceId) {
        long start = System.currentTimeMillis();
        try {
            agentTelemetry.begin();
            WorkorderResult workorder = workorderAssistant.process(anomalyDesc, orderInfo);
            AgentTelemetry.Telemetry telemetry = agentTelemetry.capture();
            long latency = System.currentTimeMillis() - start;
            JSONObject result = workorder == null ? null : JSON.parseObject(JSON.toJSONString(workorder));
            if (result == null) {
                result = fallback();
            } else if (!result.containsKey("degraded")) {
                result.put("degraded", false);
            }
            JSONObject input = new JSONObject();
            input.put("anomalyDesc", anomalyDesc);
            input.put("orderInfo", orderInfo);
            agentCallLogService.record(AGENT_NAME, input, result, telemetry.toolsCalled(),
                    telemetry.getTotalTokens() > 0 ? telemetry.getTotalTokens() : null, latency, "success", traceId);
            return result;
        } catch (Exception e) {
            agentTelemetry.capture();
            long latency = System.currentTimeMillis() - start;
            log.error("[{}] LLM 调用失败，降级到默认工单", AGENT_NAME, e);
            JSONObject fallback = fallback();
            agentCallLogService.record(AGENT_NAME, null, fallback, null, null, latency, "degraded", traceId);
            return fallback;
        }
    }

    private JSONObject fallback() {
        JSONObject fallback = new JSONObject();
        fallback.put("type", "customs_delay");
        fallback.put("level", "P1");
        fallback.put("sop", "1.联系物流商核实 2.通知买家 3.跟进处理结果");
        fallback.put("degraded", true);
        return fallback;
    }
}
