package com.java3y.austin.agent.agent;

import com.java3y.austin.agent.assistant.ContentGenAssistant;
import com.java3y.austin.agent.config.AgentTelemetry;
import com.java3y.austin.agent.service.AgentCallLogService;
import com.java3y.austin.agent.tool.TemplateTool;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * ContentGenAgent 测试：模板优先 + LLM 降级策略
 */
@ExtendWith(MockitoExtension.class)
public class ContentGenAgentDegradeTest {

    @Mock
    private ContentGenAssistant contentGenAssistant;
    @Mock
    private TemplateTool templateTool;
    @Mock
    private AgentCallLogService agentCallLogService;
    @Mock
    private AgentTelemetry agentTelemetry;
    @InjectMocks
    private ContentGenAgent contentGenAgent;

    @Test
    void templateHitShortCircuitsLlm() {
        when(templateTool.queryTemplate("IN_TRANSIT", "en")).thenReturn("Your package is in transit.");

        String result = contentGenAgent.generate("IN_TRANSIT", "en", "商品信息", "OT001", "trace-1");

        assertEquals("Your package is in transit.", result);
        verify(contentGenAssistant, never()).generate(anyString(), anyString(), anyString(), anyString());
        verify(agentCallLogService).record(eq("ContentGenAgent"), any(), eq("Your package is in transit."),
                isNull(), isNull(), anyLong(), eq("success"), eq("trace-1"));
    }

    @Test
    void llmFailureDegradesToDefaultTemplate() {
        when(templateTool.queryTemplate("IN_TRANSIT", "en")).thenReturn(null);
        when(contentGenAssistant.generate("IN_TRANSIT", "en", "商品信息", "OT001"))
                .thenThrow(new RuntimeException("LLM 不可用"));

        String result = contentGenAgent.generate("IN_TRANSIT", "en", "商品信息", "OT001", "trace-1");

        assertEquals("您的订单状态：IN_TRANSIT", result);
        verify(agentCallLogService).record(eq("ContentGenAgent"), isNull(), eq(result),
                isNull(), isNull(), anyLong(), eq("degraded"), eq("trace-1"));
    }
}
