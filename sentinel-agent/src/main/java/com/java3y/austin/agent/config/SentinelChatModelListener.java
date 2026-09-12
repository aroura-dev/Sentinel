package com.java3y.austin.agent.config;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.chat.listener.ChatModelListener;
import dev.langchain4j.model.chat.listener.ChatModelResponseContext;
import dev.langchain4j.model.output.TokenUsage;
import org.springframework.stereotype.Component;

/**
 * QwenChatModel 调用监听器：采集 token 消耗与 Tool 调用
 * <p>
 * 注册到 ChatLanguageModel，每次模型返回时把 {@code tokenUsage} 与 Tool 执行请求
 * 写入 {@link AgentTelemetry}，供 Agent 随 agent_call_log 落库，实现可观测性。
 *
 * @author sentinel
 */
@Component
public class SentinelChatModelListener implements ChatModelListener {

    private final AgentTelemetry agentTelemetry;

    public SentinelChatModelListener(AgentTelemetry agentTelemetry) {
        this.agentTelemetry = agentTelemetry;
    }

    @Override
    public void onResponse(ChatModelResponseContext context) {
        TokenUsage usage = context.response().tokenUsage();
        if (usage != null) {
            agentTelemetry.onTokenUsage(usage.totalTokenCount());
        }
        AiMessage aiMessage = context.response().aiMessage();
        if (aiMessage != null && aiMessage.hasToolExecutionRequests()) {
            for (ToolExecutionRequest request : aiMessage.toolExecutionRequests()) {
                agentTelemetry.onToolCalled(request.name());
            }
        }
    }
}
