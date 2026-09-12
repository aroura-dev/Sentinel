package com.aroura.sentinel.agent.agent;

import com.alibaba.fastjson2.JSONObject;
import com.aroura.sentinel.agent.assistant.AnomalyDiagnoseAssistant;
import com.aroura.sentinel.agent.config.AgentTelemetry;
import com.aroura.sentinel.agent.service.AgentCallLogService;
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
public class AnomalyDiagnoseAgentDegradeTest {

    @Mock
    private AnomalyDiagnoseAssistant assistant;
    @Mock
    private AgentCallLogService agentCallLogService;
    @Mock
    private AgentTelemetry agentTelemetry;
    @InjectMocks
    private AnomalyDiagnoseAgent agent;

    @Test
    void llmFailureDegradesToDefaultDiagnosis() {
        when(assistant.diagnose("customs_delay", "CUS-1102", "{}"))
                .thenThrow(new RuntimeException("LLM 不可用"));

        JSONObject result = agent.diagnose("customs_delay", "CUS-1102", "{}", "trace-1");

        assertEquals("异常原因待人工确认", result.getString("reason"));
        assertEquals("P1", result.getString("priority"));
        verify(agentCallLogService).record(eq("AnomalyDiagnoseAgent"), isNull(), any(),
                isNull(), isNull(), anyLong(), eq("degraded"), eq("trace-1"));
    }
}
