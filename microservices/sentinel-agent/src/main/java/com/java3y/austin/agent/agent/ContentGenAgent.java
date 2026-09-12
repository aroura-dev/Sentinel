package com.java3y.austin.agent.agent;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.java3y.austin.agent.assistant.ContentGenAssistant;
import com.java3y.austin.agent.config.AgentTelemetry;
import com.java3y.austin.agent.service.AgentCallLogService;
import com.java3y.austin.agent.tool.TemplateTool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Agent 1：文案生成 Agent（ContentGenAgent）
 * <p>
 * 职责：根据物流节点 + 用户语言 + 商品信息，生成通知文案。
 * <p>
 * 设计：模板优先（复用 austin message_template）+ Agent 兜底（AiServices + Tool Calling）。
 *
 * @author sentinel
 */
@Component
public class ContentGenAgent {

    private static final Logger log = LoggerFactory.getLogger(ContentGenAgent.class);
    private static final String AGENT_NAME = "ContentGenAgent";

    @Autowired
    private ContentGenAssistant contentGenAssistant;
    @Autowired
    private TemplateTool templateTool;
    @Autowired
    private AgentCallLogService agentCallLogService;
    @Autowired
    private AgentTelemetry agentTelemetry;

    /**
     * 默认模板（LLM 不可用时的降级文案）
     */
    private String defaultTemplate(String node, String language) {
        // 国内化后买家语言统一为 zh，兜底文案固定中文（删除原通知分支）
        return "您的订单状态：" + node;
    }

    /**
     * 生成通知文案
     *
     * @param node        物流节点（LogisticsNode.codeEn）
     * @param language    买家语言（ru/en/es）
     * @param productInfo 商品信息
     * @param orderNo     订单号
     * @param traceId     链路 ID
     * @return 通知文案
     */
    public String generate(String node, String language, String productInfo, String orderNo, String traceId) {
        long start = System.currentTimeMillis();
        // 1. 模板优先：确定性查 message_template（命名约定 sentinel:{node}:{language}）
        String template = templateTool.queryTemplate(node, language);
        if (template != null && !template.trim().isEmpty()) {
            long latency = System.currentTimeMillis() - start;
            JSONObject input = new JSONObject();
            input.put("mode", "template");
            input.put("node", node);
            input.put("language", language);
            agentCallLogService.record(AGENT_NAME, input, template, null, null, latency, "success", traceId);
            return template.trim();
        }
        // 2. Agent 兜底：AiServices + Tool Calling
        try {
            agentTelemetry.begin();
            String result = contentGenAssistant.generate(node, language, productInfo, orderNo);
            AgentTelemetry.Telemetry telemetry = agentTelemetry.capture();
            long latency = System.currentTimeMillis() - start;
            JSONObject input = new JSONObject();
            input.put("node", node);
            input.put("language", language);
            input.put("productInfo", productInfo);
            input.put("orderNo", orderNo);
            agentCallLogService.record(AGENT_NAME, input, result, telemetry.toolsCalled(),
                    telemetry.getTotalTokens() > 0 ? telemetry.getTotalTokens() : null, latency, "success", traceId);
            return result == null ? defaultTemplate(node, language) : result.trim();
        } catch (Exception e) {
            agentTelemetry.capture();
            long latency = System.currentTimeMillis() - start;
            log.error("[{}] LLM 调用失败，降级到默认模板", AGENT_NAME, e);
            String fallback = defaultTemplate(node, language);
            agentCallLogService.record(AGENT_NAME, null, fallback, null, null, latency, "degraded", traceId);
            return fallback;
        }
    }
}
