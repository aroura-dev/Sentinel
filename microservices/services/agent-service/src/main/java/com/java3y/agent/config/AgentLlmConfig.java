package com.java3y.agent.config;

import java.util.Collections;

import com.java3y.austin.agent.assistant.ContentGenAssistant;
import com.java3y.austin.agent.config.SentinelChatModelListener;
import com.java3y.austin.agent.tool.TemplateTool;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.dashscope.QwenChatModel;
import dev.langchain4j.service.AiServices;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * agent-service 专用 LLM 装配（替代被排除的原 LangChain4jConfig）。
 * <p>
 * 有 DASHSCOPE_API_KEY → 真实 Qwen + AiServices(ContentGenAssistant)；
 * 无 key → 提供抛异常的兜底实现，ContentGenAgent 捕获后降级为默认文案并写 degraded 调用日志，
 * 保证 agent 进程在无 LLM Key 时仍可独立启动、闭环不中断。
 *
 * @author sentinel-ms
 */
@Configuration
public class AgentLlmConfig {

    private static final Logger log = LoggerFactory.getLogger(AgentLlmConfig.class);

    @Value("${dashscope.api-key:${DASHSCOPE_API_KEY:}}")
    private String apiKey;

    @Value("${dashscope.model-name:qwen-plus}")
    private String modelName;

    @Bean
    public ContentGenAssistant contentGenAssistant(SentinelChatModelListener listener, TemplateTool templateTool) {
        if (apiKey != null && !apiKey.trim().isEmpty()) {
            ChatLanguageModel model = QwenChatModel.builder()
                    .apiKey(apiKey.trim())
                    .modelName(modelName)
                    .listeners(Collections.singletonList(listener))
                    .build();
            return AiServices.builder(ContentGenAssistant.class)
                    .chatLanguageModel(model)
                    .tools(templateTool)
                    .build();
        }
        log.warn("[AgentLlmConfig] 未配置 DASHSCOPE_API_KEY，ContentGenAssistant 走降级 stub");
        return (node, language, productInfo, orderNo) -> {
            throw new IllegalStateException("DASHSCOPE_API_KEY 未配置，ContentGenAgent 应降级");
        };
    }
}
