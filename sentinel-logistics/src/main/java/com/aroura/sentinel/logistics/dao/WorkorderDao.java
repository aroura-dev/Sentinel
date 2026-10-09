package com.aroura.sentinel.logistics.dao;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 工单 DAO（落库 workorder）
 *
 * @author sentinel
 */
@Repository
public class WorkorderDao {

    private final JdbcTemplate jdbcTemplate;

    public WorkorderDao(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void insert(String orderNo, String type, String level, String description, String agentDiagnosis, String sop, String status) {
        jdbcTemplate.update(
                "INSERT INTO workorder (order_no, type, level, description, agent_diagnosis, sop, status) VALUES (?, ?, ?, ?, ?, ?, ?)",
                orderNo, type, level, description, agentDiagnosis, sop, status);
    }

    public Map<String, Object> queryPage(String status, String level, String orderNo, Long merchantId, int page, int perPage) {
        StringBuilder where = new StringBuilder(" WHERE is_deleted = 0");
        List<Object> args = new ArrayList<>();
        if (status != null && !status.trim().isEmpty()) {
            where.append(" AND status = ?");
            args.add(status.trim());
        }
        if (level != null && !level.trim().isEmpty()) {
            where.append(" AND level = ?");
            args.add(level.trim());
        }
        if (orderNo != null && !orderNo.trim().isEmpty()) {
            where.append(" AND order_no = ?");
            args.add(orderNo.trim());
        }
        // 商家视角：只可见自己订单的工单（企业数据隔离）
        if (merchantId != null) {
            where.append(" AND order_no IN (SELECT order_no FROM logistics_order WHERE merchant_id = ? AND is_deleted = 0)");
            args.add(merchantId);
        }
        String whereSql = where.toString();
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM workorder" + whereSql, Integer.class, args.toArray());
        List<Object> pageArgs = new ArrayList<>(args);
        pageArgs.add(perPage);
        pageArgs.add((page - 1) * perPage);
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT * FROM workorder" + whereSql + " ORDER BY id DESC LIMIT ? OFFSET ?", pageArgs.toArray());
        Map<String, Object> result = new HashMap<>(4);
        result.put("count", count == null ? 0 : count);
        result.put("rows", rows);
        return result;
    }

    public Map<String, Object> queryById(Long id) {
        List<Map<String, Object>> list = jdbcTemplate.queryForList(
                "SELECT * FROM workorder WHERE id = ? AND is_deleted = 0", id);
        return list.isEmpty() ? null : list.get(0);
    }

    public void updateStatus(Long id, String status) {
        jdbcTemplate.update("UPDATE workorder SET status = ? WHERE id = ? AND is_deleted = 0", status, id);
    }

    public void updateClaim(Long id, String liability, java.math.BigDecimal claimAmount,
                            java.math.BigDecimal compensationAmount, Integer slaBreach,
                            String resolution, java.util.Date resolvedAt) {
        // 工单页「登记索赔」即发起理赔流程：置 SUBMITTED 进入待审批
        jdbcTemplate.update(
                "UPDATE workorder SET liability=?, claim_amount=?, compensation_amount=?, sla_breach=?, resolution=?, resolved_at=?, "
                        + "claim_status='SUBMITTED', claim_submitted_at=IFNULL(claim_submitted_at, ?) "
                        + "WHERE id=? AND is_deleted=0",
                liability, claimAmount, compensationAmount, slaBreach, resolution, resolvedAt, resolvedAt, id);
    }

    public void updateDiagnosis(Long id, String agentDiagnosis) {
        jdbcTemplate.update("UPDATE workorder SET agent_diagnosis=? WHERE id=? AND is_deleted=0", agentDiagnosis, id);
    }

    public Map<String, Object> stats() {
        Map<String, Object> result = new HashMap<>(8);
        result.put("openWorkorderCount", jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM workorder WHERE is_deleted = 0 AND status = 'OPEN'", Integer.class));
        result.put("p0Count", jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM workorder WHERE is_deleted = 0 AND status = 'OPEN' AND level = 'P0'", Integer.class));
        result.put("p1Count", jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM workorder WHERE is_deleted = 0 AND status = 'OPEN' AND level = 'P1'", Integer.class));
        result.put("p2Count", jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM workorder WHERE is_deleted = 0 AND status = 'OPEN' AND level = 'P2'", Integer.class));
        return result;
    }

    public boolean existsByOrderNo(String orderNo) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM workorder WHERE order_no = ? AND is_deleted = 0", Integer.class, orderNo);
        return count != null && count > 0;
    }

    // ===================== 理赔流程（复用 workorder 作为索赔载体） =====================

    /** 理赔单同样落在 workorder 表：type=CLAIM/异常类型，claim_status 驱动流程 */
    public void insertClaim(String orderNo, String type, String level, String description,
                            String liability, java.math.BigDecimal claimAmount) {
        jdbcTemplate.update(
                "INSERT INTO workorder (order_no, type, level, description, status, liability, "
                        + "claim_amount, compensation_amount, currency, claim_status, claim_submitted_at) "
                        + "VALUES (?, ?, ?, ?, 'PROCESSING', ?, ?, 0, 'CNY', 'SUBMITTED', CURRENT_TIMESTAMP)",
                orderNo, type, level, description, liability, claimAmount);
    }

    /** 理赔分页：带订单概要（商家/节点/申报/收件人/运单号） */
    public Map<String, Object> claimPage(String claimStatus, String orderNo, int page, int perPage) {
        StringBuilder where = new StringBuilder(
                " WHERE w.is_deleted = 0 AND (w.claim_status IS NOT NULL AND w.claim_status <> 'NONE' "
                        + "OR w.claim_amount > 0 OR w.compensation_amount > 0)");
        List<Object> args = new ArrayList<>();
        if (claimStatus != null && !claimStatus.trim().isEmpty()) {
            where.append(" AND w.claim_status = ?");
            args.add(claimStatus.trim());
        }
        if (orderNo != null && !orderNo.trim().isEmpty()) {
            where.append(" AND w.order_no = ?");
            args.add(orderNo.trim());
        }
        String whereSql = where.toString();
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM workorder w" + whereSql, Integer.class, args.toArray());
        List<Object> pageArgs = new ArrayList<>(args);
        pageArgs.add(perPage);
        pageArgs.add(Math.max((page - 1) * perPage, 0));
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT w.*, o.merchant_name, o.current_node, o.declared_value, o.freight_cost, "
                        + "o.destination_country, o.buyer_phone, o.buyer_address, o.waybill_no "
                        + "FROM workorder w "
                        + "LEFT JOIN logistics_order o ON o.order_no = w.order_no AND o.is_deleted = 0"
                        + whereSql
                        + " ORDER BY COALESCE(w.claim_submitted_at, w.created_at) DESC LIMIT ? OFFSET ?",
                pageArgs.toArray());
        Map<String, Object> result = new HashMap<>(4);
        result.put("count", count == null ? 0 : count);
        result.put("rows", rows);
        return result;
    }

    public Map<String, Object> claimById(Long id) {
        List<Map<String, Object>> list = jdbcTemplate.queryForList(
                "SELECT w.*, o.merchant_name, o.current_node, o.declared_value, o.freight_cost, "
                        + "o.destination_country, o.buyer_phone, o.buyer_address, o.waybill_no "
                        + "FROM workorder w "
                        + "LEFT JOIN logistics_order o ON o.order_no = w.order_no AND o.is_deleted = 0 "
                        + "WHERE w.id = ? AND w.is_deleted = 0", id);
        return list.isEmpty() ? null : list.get(0);
    }

    /** 理赔状态概览：各状态件数 + 已赔付金额 */
    public Map<String, Object> claimStats() {
        Map<String, Object> result = new HashMap<>(6);
        result.put("submitted", 0);
        result.put("approved", 0);
        result.put("paid", 0);
        result.put("rejected", 0);
        List<Map<String, Object>> groups = jdbcTemplate.queryForList(
                "SELECT claim_status, COUNT(*) AS cnt FROM workorder "
                        + "WHERE is_deleted = 0 AND (claim_status IS NOT NULL AND claim_status <> 'NONE' "
                        + "OR claim_amount > 0 OR compensation_amount > 0) "
                        + "GROUP BY claim_status");
        for (Map<String, Object> g : groups) {
            String st = g.get("claim_status") == null ? "" : String.valueOf(g.get("claim_status"));
            Object cnt = g.get("cnt");
            if ("SUBMITTED".equals(st)) result.put("submitted", cnt);
            else if ("APPROVED".equals(st)) result.put("approved", cnt);
            else if ("PAID".equals(st)) result.put("paid", cnt);
            else if ("REJECTED".equals(st)) result.put("rejected", cnt);
        }
        java.math.BigDecimal paidTotal = jdbcTemplate.queryForObject(
                "SELECT COALESCE(SUM(compensation_amount), 0) FROM workorder "
                        + "WHERE is_deleted = 0 AND claim_status = 'PAID'", java.math.BigDecimal.class);
        result.put("paidTotal", paidTotal == null ? java.math.BigDecimal.ZERO : paidTotal);
        return result;
    }

    /** 订单是否已有在流程中的理赔（SUBMITTED/APPROVED） */
    public Long activeClaimId(String orderNo) {
        List<Map<String, Object>> list = jdbcTemplate.queryForList(
                "SELECT id FROM workorder WHERE order_no = ? AND claim_status IN ('SUBMITTED','APPROVED') "
                        + "AND is_deleted = 0 LIMIT 1", orderNo);
        return list.isEmpty() ? null : ((Number) list.get(0).get("id")).longValue();
    }

    public void approveClaim(Long id, java.math.BigDecimal compensationAmount) {
        jdbcTemplate.update(
                "UPDATE workorder SET compensation_amount=?, claim_status='APPROVED', claim_approved_at=CURRENT_TIMESTAMP "
                        + "WHERE id=? AND is_deleted=0",
                compensationAmount == null ? java.math.BigDecimal.ZERO : compensationAmount, id);
    }

    public void payClaim(Long id) {
        jdbcTemplate.update(
                "UPDATE workorder SET claim_status='PAID', claim_paid_at=CURRENT_TIMESTAMP "
                        + "WHERE id=? AND is_deleted=0", id);
    }

    public void rejectClaim(Long id, String reason) {
        jdbcTemplate.update(
                "UPDATE workorder SET claim_status='REJECTED', claim_reject_reason=? "
                        + "WHERE id=? AND is_deleted=0",
                reason, id);
    }
}