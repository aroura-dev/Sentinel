package com.aroura.sentinel.agent.flow;

import com.alibaba.fastjson2.JSONObject;
import com.aroura.sentinel.agent.agent.WorkorderAgent;
import com.aroura.sentinel.agent.assistant.WorkorderAssistant;
import com.aroura.sentinel.agent.config.AgentTelemetry;
import com.aroura.sentinel.agent.dao.AgentCallLogDao;
import com.aroura.sentinel.agent.service.AgentCallLogService;
import com.aroura.sentinel.logistics.dao.WorkorderDao;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import javax.sql.DataSource;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * SQL Rule-DB 自举与热更新回归测试。
 */
@SpringJUnitConfig(SentinelFlowRuleServiceTest.TestConfig.class)
@TestPropertySource(properties = {
        "sentinel.flow.rule-db.enabled=true",
        "sentinel.flow.rule-db.application-name=sentinel-test",
        "sentinel.flow.rule-db.url=jdbc:h2:mem:ruletest;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
        "sentinel.flow.rule-db.username=sa",
        "sentinel.flow.rule-db.password=",
        "sentinel.flow.rule-db.driver-class-name=org.h2.Driver",
        "sentinel.flow.rule-db.table-prefix=lf_test_",
        "sentinel.flow.rule-db.auto-init-table=true",
        "sentinel.flow.rule-db.poll-seconds=1",
        "sentinel.flow.rule-db.reconcile-seconds=5"
})
class SentinelFlowRuleServiceTest {

    @Configuration
    @ComponentScan(basePackageClasses = SentinelFlowConfig.class)
    static class TestConfig {

        @Bean
        DataSource dataSource() {
            DriverManagerDataSource dataSource = new DriverManagerDataSource();
            dataSource.setDriverClassName("org.h2.Driver");
            dataSource.setUrl("jdbc:h2:mem:ruletest;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE");
            dataSource.setUsername("sa");
            dataSource.setPassword("");
            return dataSource;
        }

        @Bean
        JdbcTemplate jdbcTemplate(DataSource dataSource) {
            return new JdbcTemplate(dataSource);
        }

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
            return mock(SentinelChannelNotifier.class);
        }
    }

    @Autowired
    private SentinelFlowService flowService;
    @Autowired
    private SentinelFlowRuleService ruleService;
    @Autowired
    private WorkorderAgent workorderAgent;
    @Autowired
    private WorkorderDao workorderDao;
    @Autowired
    private SentinelChannelNotifier channelNotifier;

    @BeforeEach
    void setUp() {
        JSONObject classified = new JSONObject();
        classified.put("type", "lost");
        classified.put("level", "P0");
        classified.put("sop", "联系承运商并升级处理");
        classified.put("degraded", false);
        when(workorderAgent.process(any(), any(), any())).thenReturn(classified);
        when(workorderDao.findLatestIdByOrderNo(anyString())).thenReturn(null);
        when(workorderDao.insertAndReturnId(anyString(), anyString(), anyString(), any(), any(),
                any(), anyString())).thenReturn(201L);
        when(channelNotifier.notifyChannels(anyString(), anyString(), anyString())).thenReturn(1);
    }

    @Test
    void bootstrapsFromDatabaseAndHotReloadsPublishedEl() {
        Map<String, Object> initial = ruleService.currentRule();
        assertNotNull(initial);
        assertEquals(1L, ((Number) initial.get("version")).longValue());

        SentinelFlowContext first = execute("OT-RULE-001");
        assertNotNull(first.getWorkOrderId());
        verify(workorderAgent, times(1)).process(any(), any(), any());
        verify(channelNotifier, times(1)).notifyChannels(anyString(), anyString(), anyString());

        Map<String, Object> published = ruleService.publish(
                "THEN(detectException, FINALLY(recordAudit))", 1L);
        assertEquals(2L, ((Number) published.get("version")).longValue());

        SentinelFlowContext second = execute("OT-RULE-002");
        assertFalse(second.isNotified());
        verify(workorderAgent, times(1)).process(any(), any(), any());
        verify(channelNotifier, times(1)).notifyChannels(anyString(), anyString(), anyString());
    }

    private SentinelFlowContext execute(String orderNo) {
        SentinelFlowContext context = new SentinelFlowContext();
        context.setOrderNo(orderNo);
        context.setAnomalyDesc("包裹轨迹长时间未更新");
        context.setTraceId("trace-" + orderNo);
        return flowService.execute(context);
    }
}
