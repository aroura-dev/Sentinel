package com.java3y.austin.agent.agent;

import com.alibaba.fastjson2.JSONObject;
import com.java3y.austin.agent.assistant.CsRouteAssistant;
import com.java3y.austin.agent.config.AgentTelemetry;
import com.java3y.austin.agent.service.AgentCallLogService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CsRouteAgentDegradeTest {

    @Mock
    private CsRouteAssistant assistant;
    @Mock
    private AgentCallLogService agentCallLogService;
    @Mock
    private AgentTelemetry agentTelemetry;
    @InjectMocks
    private CsRouteAgent agent;

    @Test
    void llmFailureDegradesToHuman() {
        when(assistant.route("我的包裹到哪了", "buyer-1")).thenThrow(new RuntimeException("LLM 不可用"));

        JSONObject result = agent.route("我的包裹到哪了", "buyer-1", "trace-1");

        assertEquals("human", result.getString("route"));
        verify(agentCallLogService).record(eq("CsRouteAgent"), isNull(), any(),
                isNull(), isNull(), anyLong(), eq("degraded"), eq("trace-1"));
    }
}
