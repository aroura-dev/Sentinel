package com.aroura.sentinel.web.service.sentinel.tms;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 风险预警自动处置规则：SLA 预警/违约 → 配置通知/建工单等自动动作
 *
 * @author sentinel
 */
@Service
public class RiskRuleService {

    private final JdbcTemplate jdbcTemplate;
    private final AuditLogService auditLogService;

    public RiskRuleService(JdbcTemplate jdbcTemplate, AuditLogService auditLogService) {
        this.jdbcTemplate = jdbcTemplate;
        this.auditLogService = auditLogService;
    }

    public List<Map<String, Object>> list() {
        return jdbcTemplate.queryForList("SELECT * FROM risk_rule WHERE is_deleted = 0 ORDER BY id DESC");
    }

    public Map<String, Object> save(String name, String scene, String triggerStatus, String action, String actionConfig, Integer enabled) {
        jdbcTemplate.update(
                "INSERT INTO risk_rule (name, scene, trigger_status, action, action_config, enabled) VALUES (?,?,?,?,?,?)",
                name, scene, triggerStatus, action, actionConfig, enabled != null ? enabled : 1);
        Long id = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        auditLogService.log("风险预警", "新增处置规则", name, "场景:" + scene + " 动作:" + action);
        return find(id);
    }

    public Map<String, Object> update(Long id, String name, String scene, String triggerStatus, String action, String actionConfig, Integer enabled) {
        jdbcTemplate.update(
                "UPDATE risk_rule SET name = ?, scene = ?, trigger_status = ?, action = ?, action_config = ?, enabled = ? WHERE id = ?",
                name, scene, triggerStatus, action, actionConfig, enabled, id);
        auditLogService.log("风险预警", "更新处置规则", name, "规则ID:" + id);
        return find(id);
    }

    public void delete(Long id) {
        jdbcTemplate.update("UPDATE risk_rule SET is_deleted = 1 WHERE id = ?", id);
        auditLogService.log("风险预警", "删除处置规则", String.valueOf(id), "");
    }

    public Map<String, Object> find(Long id) {
        List<Map<String, Object>> list = jdbcTemplate.queryForList(
                "SELECT * FROM risk_rule WHERE id = ? AND is_deleted = 0", id);
        if (list.isEmpty()) {
            throw new IllegalArgumentException("规则不存在：" + id);
        }
        return list.get(0);
    }

    /** SLA 风险订单（RISK/BREACHED） */
    public Map<String, Object> riskOrders(String slaStatus, int page, int perPage) {
        StringBuilder sql = new StringBuilder("SELECT * FROM logistics_order WHERE is_deleted = 0 AND sla_status IN ('RISK','BREACHED')");
        if (slaStatus != null && !slaStatus.isEmpty()) {
            sql.append(" AND sla_status = '").append(slaStatus).append("'");
        }
        int count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM (" + sql + ") t", Integer.class);
        int offset = Math.max((page - 1) * perPage, 0);
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                sql + " ORDER BY id DESC LIMIT " + offset + "," + perPage);
        Map<String, Object> res = new HashMap<>();
        res.put("count", count);
        res.put("rows", rows);
        return res;
    }

    /** 对某订单执行处置动作（简化：记录留痕并返回处置说明） */
    public Map<String, Object> execute(Long ruleId, String orderNo) {
        Map<String, Object> rule = find(ruleId);
        String action = String.valueOf(rule.get("action"));
        String actionLabel = "NOTIFY".equals(action) ? "自动通知相关方" : "自动创建异常工单";
        jdbcTemplate.update("UPDATE logistics_order SET sla_status = 'BREACHED' WHERE order_no = ?", orderNo);
        auditLogService.log("风险预警", "自动处置", orderNo, "规则:" + rule.get("name") + " → " + actionLabel);
        Map<String, Object> res = new HashMap<>();
        res.put("orderNo", orderNo);
        res.put("action", action);
        res.put("message", actionLabel + "已执行");
        return res;
    }
}
