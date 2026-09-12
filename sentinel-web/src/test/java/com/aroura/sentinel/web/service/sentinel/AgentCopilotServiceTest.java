package com.aroura.sentinel.web.service.sentinel;

import com.aroura.sentinel.agent.service.AgentCallLogService;
import com.aroura.sentinel.logistics.dao.KnowledgeDao;
import com.aroura.sentinel.logistics.dao.LogisticsDao;
import com.aroura.sentinel.web.service.sentinel.tms.TmsDashboardService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AgentCopilotServiceTest {

    @Mock private AgentCallLogService logService;
    @Mock private KnowledgeDao knowledgeDao;
    @Mock private LogisticsDao logisticsDao;
    @Mock private TmsDashboardService dashboardService;
    @Mock private JdbcTemplate jdbcTemplate;

    @Test
    void chatGroundsAnswerWithOrderTrackAndKnowledge() {
        when(logService.generateTraceId()).thenReturn("trace-chat");
        Map<String, Object> order = new LinkedHashMap<>();
        order.put("current_node", "CUSTOMS_DELAY");
        when(logisticsDao.findOrderByNo("OMT-1")).thenReturn(order);
        List<Map<String, Object>> tracks = new ArrayList<>();
        tracks.add(mapOf("raw_desc", "包裹中转延误"));
        when(logisticsDao.listTracks("OMT-1")).thenReturn(tracks);
        List<Map<String, Object>> knowledge = new ArrayList<>();
        knowledge.add(mapOf("id", 1, "type", "DELAY", "status_code", "CUS-1102",
                "description", "中转节点拥堵", "suggestion", "联系承运商核实"));
        when(knowledgeDao.search("延误", 3)).thenReturn(knowledge);

        AgentCopilotService service = service();
        Map<String, Object> result = service.chat("包裹延误怎么处理", "OMT-1");

        assertFalse((Boolean) result.get("degraded"));
        assertEquals("trace-chat", result.get("trace_id"));
        assertEquals(Boolean.TRUE, !String.valueOf(result.get("answer")).isEmpty());
        verify(logService).record(org.mockito.ArgumentMatchers.eq("JavaKnowledgeAgent"),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.eq("query_track,query_knowledge"),
                org.mockito.ArgumentMatchers.eq(0), org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.eq("success"), org.mockito.ArgumentMatchers.eq("trace-chat"));
    }

    @Test
    void analyticsMapsChannelQuestionToReadOnlyChannelQuery() {
        when(logService.generateTraceId()).thenReturn("trace-analytics");
        List<Map<String, Object>> rows = new ArrayList<>();
        rows.add(mapOf("label", "海运专线", "cnt", 8L));
        when(jdbcTemplate.queryForList(anyString())).thenReturn(rows);

        AgentCopilotService service = service();
        Map<String, Object> result = service.analytics("各渠道订单量");

        assertEquals("trace-analytics", result.get("trace_id"));
        assertEquals(Boolean.FALSE, result.get("degraded"));
        Map<?, ?> data = (Map<?, ?>) result.get("data");
        assertEquals(2, ((List<?>) data.get("columns")).size());
        assertEquals(1, ((List<?>) data.get("rows")).size());
        verify(logService).record(org.mockito.ArgumentMatchers.eq("JavaAnalyticsAgent"),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.eq("read_metrics"),
                org.mockito.ArgumentMatchers.eq(0), org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.eq("success"), org.mockito.ArgumentMatchers.eq("trace-analytics"));
    }

    private AgentCopilotService service() {
        return new AgentCopilotService(logService, knowledgeDao, logisticsDao, dashboardService, jdbcTemplate);
    }

    private Map<String, Object> mapOf(Object... values) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (int i = 0; i + 1 < values.length; i += 2) {
            result.put(String.valueOf(values[i]), values[i + 1]);
        }
        return result;
    }
}
