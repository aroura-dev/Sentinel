package com.java3y.austin.agent.flow;

import com.alibaba.fastjson2.JSONObject;
import com.java3y.austin.agent.agent.WorkorderAgent;
import com.java3y.austin.agent.assistant.WorkorderAssistant;
import com.java3y.austin.agent.config.AgentTelemetry;
import com.java3y.austin.agent.dao.AgentCallLogDao;
import com.java3y.austin.agent.service.AgentCallLogService;
import com.java3y.austin.logistics.dao.WorkorderDao;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * LiteFlow 异常处置链回归测试。
 */
@SpringJUnitConfig(SentinelFlowServiceTest.TestConfig.class)
class SentinelFlowServiceTest {

    @Configuration
    @ComponentScan(basePackageClasses = SentinelFlowConfig.class)
    static class TestConfig {

        @Bean
        WorkorderAgent workorderAgent() {
            return mock(WorkorderAgent.class);
        }

        @Bean
        WorkorderAssistant workorderAssistant() {
            return mock(WorkorderAssistant.class);
        }

        @Bean
        AgentCallLogDao agentCallLogDao() {
            return mock(AgentCallLogDao.class);
        }

        @Bean
        AgentCallLogService agentCallLogService() {
            return new AgentCallLogService();
        }

        @Bean
        AgentTelemetry agentTelemetry() {
            return new AgentTelemetry();
        }

        @Bean
        WorkorderDao workorderDao() {
            return mock(WorkorderDao.class);
        }

        @Bean
        SentinelChannelNotifier channelNotifier() {
            return (orderNo, node, traceId) -> 1;
        }
    }

    @Autowired
    private SentinelFlowService flowService;
    @Autowired
    private WorkorderAgent workorderAgent;
    @Autowired
    private WorkorderDao workorderDao;

    @BeforeEach
    void setUp() {
        JSONObject classified = new JSONObject();
        classified.put("type", "customs_delay");
        classified.put("level", "P1");
        classified.put("sop", "联系物流商核实并通知买家");
        classified.put("degraded", false);
        when(workorderAgent.process(any(), any(), any())).thenReturn(classified);
        when(workorderDao.findLatestIdByOrderNo(anyString())).thenReturn(null);
        when(workorderDao.insertAndReturnId(anyString(), anyString(), anyString(), any(), any(),
                any(), anyString())).thenReturn(101L);
    }

    @Test
    void executesNormalFlowAndRecordsAudit() {
        SentinelFlowContext context = new SentinelFlowContext();
        context.setOrderNo("OT-FLOW-001");
        context.setAnomalyDesc("包裹在分拨中心停留超过 24 小时");
        context.setTraceId("trace-flow-001");

        SentinelFlowContext result = flowService.execute(context);

        assertNotNull(result);
        assertTrue(result.isNotified());
        assertFalse(result.isDegraded());
        assertEquals("customs_delay", result.getExceptionType());
        assertEquals("P1", result.getPriority());
        assertEquals(101L, result.getWorkOrderId());
    }

    @Test
    void degradesWhenConfidenceIsLow() {
        SentinelFlowContext context = new SentinelFlowContext();
        context.setOrderNo("OT-FLOW-002");
        context.setAnomalyDesc("轨迹异常但无法确定原因");
        context.setLowConfidence(true);
        context.setTraceId("trace-flow-002");

        SentinelFlowContext result = flowService.execute(context);

        assertNotNull(result);
        assertTrue(result.isNotified());
        assertTrue(result.isDegraded());
        assertEquals(101L, result.getWorkOrderId());
    }

    @Test
    void catchesDetectionFailureAndStillAudits() {
        SentinelFlowContext context = new SentinelFlowContext();
        context.setOrderNo("OT-FLOW-003");
        context.setTraceId("trace-flow-003");

        SentinelFlowContext result = flowService.execute(context);

        assertNotNull(result);
        assertTrue(result.isDegraded());
        assertFalse(result.isNotified());
    }
}
