package com.aroura.sentinel.web.service.sentinel;

import com.alibaba.fastjson2.JSONObject;
import com.aroura.sentinel.agent.agent.AnomalyDiagnoseAgent;
import com.aroura.sentinel.agent.agent.WorkorderAgent;
import com.aroura.sentinel.agent.service.AgentCallLogService;
import com.aroura.sentinel.logistics.dao.WorkorderDao;
import com.aroura.sentinel.logistics.dao.OutboxEventDao;
import org.springframework.transaction.annotation.Transactional;
import com.aroura.sentinel.logistics.enums.WorkOrderState;
import com.aroura.sentinel.logistics.enums.WorkOrderStatePermission;
import com.aroura.sentinel.logistics.enums.WorkOrderStateTransition;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Date;
import java.util.Map;

/**
 * 异常工单服务
 * <p>
 * 从 SentinelWorkorderController 抽取：Agent 处理建单、推送、状态流转。
 *
 * @author sentinel
 */
@Service
public class WorkorderService {

    private final WorkorderDao workorderDao;
    private final WorkorderAgent workorderAgent;
    private final AnomalyDiagnoseAgent anomalyDiagnoseAgent;
    private final AgentCallLogService agentCallLogService;
    private final com.aroura.sentinel.web.service.sentinel.tms.AuditLogService auditLogService;
    private final OutboxEventDao outboxEventDao;

    public WorkorderService(WorkorderDao workorderDao,
                            WorkorderAgent workorderAgent,
                            AnomalyDiagnoseAgent anomalyDiagnoseAgent,
                            AgentCallLogService agentCallLogService,
                            com.aroura.sentinel.web.service.sentinel.tms.AuditLogService auditLogService,
                            OutboxEventDao outboxEventDao) {
        this.workorderDao = workorderDao;
        this.workorderAgent = workorderAgent;
        this.anomalyDiagnoseAgent = anomalyDiagnoseAgent;
        this.agentCallLogService = agentCallLogService;
        this.auditLogService = auditLogService;
        this.outboxEventDao = outboxEventDao;
    }

    public Map<String, Object> stats() {
        return workorderDao.stats();
    }

    public Map<String, Object> list(String status, String level, String orderNo, Long merchantId, int page, int perPage) {
        return workorderDao.queryPage(status, level, orderNo, merchantId, page, perPage);
    }

    public Map<String, Object> detail(Long id) {
        return workorderDao.queryById(id);
    }

    public boolean push(Long workorderId) {
        Map<String, Object> row = workorderDao.queryById(workorderId);
        if (row == null) {
            return false;
        }
        workorderDao.updateStatus(workorderId, "PUSHED");
        return true;
    }

    /**
     * Agent 处理异常并创建工单
     */
    public Map<String, Object> process(String orderNo, String anomalyDesc) {
        String traceId = agentCallLogService.generateTraceId();
        JSONObject result = workorderAgent.process(anomalyDesc, orderNo, traceId);
        String type = result.getString("type");
        String level = result.getString("level");
        String sop = result.getString("sop");
        workorderDao.insert(orderNo, type, level, anomalyDesc, result.toJSONString(), sop, "OPEN");
        return workorderDao.queryPage(null, null, null, null, 1, 1);
    }

    /**
     * AI 诊断工单：调用 AnomalyDiagnoseAgent，持久化 agent_diagnosis，返回 {reason, suggestion, priority}
     */
    public JSONObject diagnose(Long id) {
        Map<String, Object> row = workorderDao.queryById(id);
        if (row == null) {
            return null;
        }
        String orderNo = row.get("order_no") == null ? "" : String.valueOf(row.get("order_no"));
        String type = row.get("type") == null ? "DELIVERY_FAILED" : String.valueOf(row.get("type"));
        String desc = row.get("description") == null ? "物流异常" : String.valueOf(row.get("description"));
        String traceId = agentCallLogService.generateTraceId();
        JSONObject result = anomalyDiagnoseAgent.diagnose(type, type, "订单 " + orderNo + "： " + desc, traceId);
        workorderDao.updateDiagnosis(id, result.toJSONString());
        result.put("traceId", traceId);
        auditLogService.log("workorder", "DIAGNOSE", orderNo,
                "工单#" + id + " AI诊断 优先级=" + result.getString("priority") + " traceId=" + traceId);
        return result;
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean updateStatus(Long id, String status) {
        Map<String, Object> row = workorderDao.queryStateById(id);
        if (row == null) {
            return false;
        }
        String current = String.valueOf(row.get("status"));
        String target = status == null ? "" : status.trim().toUpperCase();
        if (current.equalsIgnoreCase(target)) {
            // 幂等：目标状态与当前一致，直接视为成功
            return true;
        }
        WorkOrderState from = WorkOrderState.fromCode(current);
        WorkOrderState to = WorkOrderState.fromCode(target);
        if (to == null) {
            throw new IllegalArgumentException("不支持的目标状态: " + status);
        }
        // 状态机校验：非法迁移直接拒绝
        WorkOrderStateTransition.requireValid(from, to);
        // P0-3 权限矩阵：角色与目标状态必须匹配（无请求上下文视为系统流转）
        WorkOrderStatePermission.requireAllowed(auditLogService.currentRole(), to);
        int updated = workorderDao.updateStatusWithVersion(id, current, to.getCode());
        if (updated == 0) {
            throw new IllegalStateException("工单状态已被并发修改，请刷新后重试");
        }
        auditLogService.log("workorder", "STATE_CHANGE", String.valueOf(row.get("order_no")),
                "工单#" + id + " 状态 " + current + " -> " + to.getCode());
        // 同一事务写入 outbox：业务落库与事件投递保持一致
        String eventId = java.util.UUID.randomUUID().toString();
        JSONObject payload = new JSONObject();
        payload.put("eventId", eventId);
        payload.put("workOrderId", id);
        payload.put("orderNo", String.valueOf(row.get("order_no")));
        payload.put("from", current);
        payload.put("to", to.getCode());
        payload.put("changedAt", System.currentTimeMillis());
        outboxEventDao.insert(eventId,
                "WorkOrderStateChanged", "workorder", String.valueOf(row.get("order_no")),
                payload.toJSONString());
        return true;
    }

    /**
     * 登记索赔：责任方/索赔额/理赔额，工单置为 RESOLVED
     */
    public boolean claim(Long id, String liability, BigDecimal claimAmount,
                         BigDecimal compensationAmount, String resolution) {
        Map<String, Object> row = workorderDao.queryById(id);
        if (row == null) {
            return false;
        }
        workorderDao.updateClaim(id, liability,
                claimAmount == null ? BigDecimal.ZERO : claimAmount,
                compensationAmount == null ? BigDecimal.ZERO : compensationAmount,
                1, resolution, new Date());
        workorderDao.updateStatus(id, "RESOLVED");
        String orderNo = row.get("order_no") == null ? "" : String.valueOf(row.get("order_no"));
        auditLogService.log("workorder", "CLAIM", orderNo,
                "工单#" + id + " 责任方=" + liability + " 索赔=" + claimAmount + " 理赔=" + compensationAmount);
        return true;
    }
}
