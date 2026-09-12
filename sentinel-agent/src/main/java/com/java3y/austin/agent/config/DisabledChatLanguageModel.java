package com.java3y.austin.agent.config;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.output.Response;

import java.util.List;

/**
 * 未配置模型 Key 时使用的空实现。
 *
 * <p>它只负责让 Spring 容器和 AiServices 代理正常装配；真正调用时抛出异常，
 * 由各 Agent 统一捕获并切换到模板或转人工的确定性降级路径。</p>
 */
public class DisabledChatLanguageModel implements ChatLanguageModel {

    @Override
    public Response<dev.langchain4j.data.message.AiMessage> generate(List<ChatMessage> messages) {
        throw new IllegalStateException("DASHSCOPE_API_KEY 未配置，模型调用已禁用并触发降级");
    }
}
