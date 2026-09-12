package com.aroura.sentinel.web.service.sentinel;

import com.aroura.sentinel.agent.service.AgentCallLogService;
import com.aroura.sentinel.logistics.dao.LogisticsDao;
import com.aroura.sentinel.web.service.SentinelNotifyService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AgentOpsServiceTest {

    @Mock private AgentCallLogService logService;
    @Mock private LogisticsDao logisticsDao;
    @Mock private SentinelNotifyService notifyService;
    @Mock private WorkorderService workorderService;

    @Test
    void canRejectPendingProposalAfterServiceRestart() {
        when(logService.queryById(7L)).thenReturn(pendingRow("notification/send"));
        AgentOpsService service = service();

        Map<String, Object> result = service.reject(7L, "运营人工拒绝");

        assertEquals("trace-ops", result.get("traceId"));
        verify(logService).updateStatus(eq(7L), eq("failed"), anyString());
    }

    @Test
    void canApprovePersistedProposalUsingDatabasePayload() {
        when(logService.queryById(8L)).thenReturn(pendingRow("notification/send"));
        Map<String, Object> notifyResult = new LinkedHashMap<>();
        notifyResult.put("status", "SENT");
        when(notifyService.send("OMT-1", "CUSTOMS_DELAY", "buyer", "push")).thenReturn(notifyResult);
        when(logService.pendingCount("trace-ops")).thenReturn(0);
        AgentOpsService service = service();

        Map<String, Object> result = service.approve(8L, "运营人工通过");

        assertEquals("success", result.get("resultStatus"));
        verify(notifyService).send("OMT-1", "CUSTOMS_DELAY", "buyer", "push");
        verify(logService).updateStatus(eq(8L), eq("success"), anyString());
    }

    private Map<String, Object> pendingRow(String action) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", 7L);
        row.put("status", "pending_approval");
        row.put("trace_id", "trace-ops");
        row.put("output", "{\"action\":\"" + action + "\",\"params\":{\"orderNo\":\"OMT-1\","
                + "\"node\":\"CUSTOMS_DELAY\",\"role\":\"buyer\",\"channel\":\"push\"}}");
        return row;
    }

    private AgentOpsService service() {
        return new AgentOpsService(logService, logisticsDao, notifyService, workorderService);
    }
}
