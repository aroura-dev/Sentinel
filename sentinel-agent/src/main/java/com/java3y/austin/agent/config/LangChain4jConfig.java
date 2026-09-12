package com.java3y.austin.agent.config;

import com.java3y.austin.agent.assistant.AnomalyDiagnoseAssistant;
import com.java3y.austin.agent.assistant.ContentGenAssistant;
import com.java3y.austin.agent.assistant.CsRouteAssistant;
import com.java3y.austin.agent.assistant.EtaPredictAssistant;
import com.java3y.austin.agent.assistant.RouteAdviceAssistant;
import com.java3y.austin.agent.assistant.WorkorderAssistant;
import com.java3y.austin.agent.tool.AnomalyKnowledgeTool;
import com.java3y.austin.agent.tool.ChannelQuoteTool;
import com.java3y.austin.agent.tool.EtaPredictTool;
import com.java3y.austin.agent.tool.LogisticsQueryTool;
import com.java3y.austin.agent.tool.TemplateTool;
import com.java3y.austin.agent.tool.WorkorderTool;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.dashscope.QwenChatModel;
import dev.langchain4j.service.AiServices;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Collections;

/**
 * LangChain4j 配置
 * <p>
 * 集成通义千问（DashScope），通过 langchain4j-dashscope 适配。
 * 6 个 Agent 统一以 AiServices + Tool Calling 编排：这里装配 ChatLanguageModel 与
 * 各 Assistant 代理，Agent 层只负责超时/降级与 agent_call_log 落库。
 * <p>
 * 兼容 Spring Boot 2.5.6 + Java 8。
 *
 * @author sentinel
 */
@Configuration
public class LangChain4jConfig {

    /**
     * 通义千问 API Key（从环境变量或配置文件读取）
     */
    @Value("${dashscope.api-key:${DASHSCOPE_API_KEY:}}")
    private String apiKey;

    /**
     * 模型名称（默认 qwen-plus，可按需切换 qwen-turbo/qwen-max）
     */
    @Value("${dashscope.model-name:qwen-plus}")
    private String modelName;

    /**
     * ChatLanguageModel：所有 Agent 通过该 Bean 调用通义千问，并注册遥测监听器
     */
    @Bean
    public ChatLanguageModel chatLanguageModel(SentinelChatModelListener listener) {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            return new DisabledChatLanguageModel();
        }
        return QwenChatModel.builder()
                .apiKey(apiKey)
                .modelName(modelName)
                .listeners(Collections.singletonList(listener))
                .build();
    }

    /* ============ 6 个 AiServices Assistant ============ */

    @Bean
    public ContentGenAssistant contentGenAssistant(ChatLanguageModel model, TemplateTool templateTool) {
        AgentToolWhitelist.assertAllowed("ContentGenAssistant", templateTool);
        return AiServices.builder(ContentGenAssistant.class)
                .chatLanguageModel(model)
                .tools(templateTool)
                .build();
    }

    @Bean
    public AnomalyDiagnoseAssistant anomalyDiagnoseAssistant(ChatLanguageModel model, AnomalyKnowledgeTool knowledgeTool) {
        AgentToolWhitelist.assertAllowed("AnomalyDiagnoseAssistant", knowledgeTool);
        return AiServices.builder(AnomalyDiagnoseAssistant.class)
                .chatLanguageModel(model)
                .tools(knowledgeTool)
                .build();
    }

    @Bean
    public WorkorderAssistant workorderAssistant(ChatLanguageModel model, WorkorderTool workorderTool, AnomalyKnowledgeTool knowledgeTool) {
        AgentToolWhitelist.assertAllowed("WorkorderAssistant", workorderTool);
        return AiServices.builder(WorkorderAssistant.class)
                .chatLanguageModel(model)
                .tools(workorderTool, knowledgeTool)
                .build();
    }

    @Bean
    public CsRouteAssistant csRouteAssistant(ChatLanguageModel model, LogisticsQueryTool logisticsQueryTool) {
        AgentToolWhitelist.assertAllowed("CsRouteAssistant", logisticsQueryTool);
        return AiServices.builder(CsRouteAssistant.class)
                .chatLanguageModel(model)
                .tools(logisticsQueryTool)
                .build();
    }

    @Bean
    public RouteAdviceAssistant routeAdviceAssistant(ChatLanguageModel model, ChannelQuoteTool channelQuoteTool) {
        AgentToolWhitelist.assertAllowed("RouteAdviceAssistant", channelQuoteTool);
        return AiServices.builder(RouteAdviceAssistant.class)
                .chatLanguageModel(model)
                .tools(channelQuoteTool)
                .build();
    }

    @Bean
    public EtaPredictAssistant etaPredictAssistant(ChatLanguageModel model, EtaPredictTool etaPredictTool) {
        AgentToolWhitelist.assertAllowed("EtaPredictAssistant", etaPredictTool);
        return AiServices.builder(EtaPredictAssistant.class)
                .chatLanguageModel(model)
                .tools(etaPredictTool)
                .build();
    }
}
