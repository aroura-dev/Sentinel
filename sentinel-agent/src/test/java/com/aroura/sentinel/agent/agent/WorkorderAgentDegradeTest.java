package com.aroura.sentinel.agent.agent;

import com.alibaba.fastjson2.JSONObject;
import com.aroura.sentinel.agent.assistant.WorkorderAssistant;
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
public class WorkorderAgentDegradeTest {

    @Mock
    private WorkorderAssistant assistant;
    @Mock
    private AgentCallLogService agentCallLogService;
    @Mock
    private AgentTelemetry agentTelemetry;
    @InjectMocks
    private WorkorderAgent agent;

    @Test
    void llmFailureDegradesToDefaultWorkorder() {
        when(assistant.process("异常描述", "OT001")).thenThrow(new RuntimeException("LLM 不可用"));

        JSONObject result = agent.process("异常描述", "OT001", "trace-1");

        assertEquals("customs_delay", result.getString("type"));
        assertEquals("P1", result.getString("level"));
        verify(agentCallLogService).record(eq("WorkorderAgent"), isNull(), any(),
                isNull(), isNull(), anyLong(), eq("degraded"), eq("trace-1"));
    }
}
