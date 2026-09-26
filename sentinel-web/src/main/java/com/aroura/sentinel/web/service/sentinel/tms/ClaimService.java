package com.aroura.sentinel.web.service.sentinel.tms;

import org.springframework.transaction.annotation.Transactional;
import com.aroura.sentinel.logistics.dao.WorkorderDao;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * 理赔流程服务：索赔登记 → 责任认定审批 → 赔付执行 / 驳回（最小实现，复用 workorder 承载）
 * <p>
 * 售后闭环：售后退货(after_sale) + 问题工单(workorder) + 理赔流程(本类)。
 * 理赔来源有二：① 工单页「登记索赔」；② 理赔流程页直接登记，都会进入 SUBMITTED → 审批 → 赔付。
 *
 * @author sentinel
 */
@Service
public class ClaimService {

    private final WorkorderDao workorderDao;
    private final JdbcTemplate jdbcTemplate;
    private final AuditLogService auditLogService;

    public ClaimService(WorkorderDao workorderDao, JdbcTemplate jdbcTemplate, AuditLogService auditLogService) {
        this.workorderDao = workorderDao;
        this.jdbcTemplate = jdbcTemplate;
        this.auditLogService = auditLogService;
    }

    public Map<String, Object> list(String claimStatus, String orderNo, int page, int perPage) {
        return workorderDao.claimPage(claimStatus, orderNo, page, perPage);
    }

    public Map<String, Object> detail(Long id) {
        Map<String, Object> row = workorderDao.claimById(id);
        if (row == null) {
            throw new IllegalArgumentException("理赔单不存在：" + id);
        }
        return row;
    }

    public Map<String, Object> stats() {
        return workorderDao.claimStats();
    }

    /** 登记理赔：校验订单存在、无进行中的理赔，再落一条工单进入待审批 */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> register(String orderNo, String type, String description,
                                        String liability, BigDecimal claimAmount) {
        if (orderNo == null || orderNo.trim().isEmpty()) {
            throw new IllegalArgumentException("请填写订单号");
        }
        Integer orderCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM logistics_order WHERE order_no = ? AND is_deleted = 0", Integer.class, orderNo.trim());
        if (orderCount == null || orderCount == 0) {
            throw new IllegalArgumentException("订单不存在：" + orderNo);
        }
        Long active = workorderDao.activeClaimId(orderNo.trim());
        if (active != null) {
            throw new IllegalArgumentException("该订单已有理赔在流程中（工单 #" + active + "），请先在理赔流程中处理");
        }
        BigDecimal amount = claimAmount == null ? BigDecimal.ZERO : claimAmount;
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("请填写大于 0 的索赔金额");
        }
        String t = (type == null || type.isEmpty()) ? "CLAIM" : type;
        String liabilityFix = (liability == null || liability.isEmpty()) ? "carrier" : liability;
        workorderDao.insertClaim(orderNo.trim(), t, "P1", description, liabilityFix, amount);
        auditLogService.log("理赔流程", "登记索赔", orderNo.trim(),
                "责任方=" + liabilityFix + " 索赔金额=" + amount + " 进入待审批");
        Map<String, Object> res = new HashMap<>(4);
        res.put("orderNo", orderNo.trim());
        res.put("status", "SUBMITTED");
        return res;
    }

    /** 审批通过：确定核定赔付金额，进入「待赔付」 */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> approve(Long id, BigDecimal compensationAmount) {
        Map<String, Object> row = mustClaim(id);
        requireStatus(row, "SUBMITTED", "审批通过");
        BigDecimal comp = compensationAmount == null ? BigDecimal.ZERO : compensationAmount;
        if (comp.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("核定赔付金额不能为负");
        }
        workorderDao.approveClaim(id, comp);
        auditLogService.log("理赔流程", "审批通过", str(row, "order_no"),
                "工单#" + id + " 核定赔付=" + comp + " 进入待赔付");
        return done(id, "APPROVED");
    }

    /** 执行赔付：待赔付 → 已赔付 */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> pay(Long id) {
        Map<String, Object> row = mustClaim(id);
        requireStatus(row, "APPROVED", "确认赔付");
        workorderDao.payClaim(id);
        auditLogService.log("理赔流程", "赔付完成", str(row, "order_no"),
                "工单#" + id + " 赔付=" + str(row, "compensation_amount"));
        return done(id, "PAID");
    }

    /** 驳回：待审批/待赔付均可驳回，记录驳回原因 */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> reject(Long id, String reason) {
        Map<String, Object> row = mustClaim(id);
        String st = str(row, "claim_status");
        if (!"SUBMITTED".equals(st) && !"APPROVED".equals(st)) {
            throw new IllegalArgumentException("当前状态(" + st + ")不允许驳回");
        }
        if (reason == null || reason.trim().isEmpty()) {
            throw new IllegalArgumentException("请填写驳回原因");
        }
        workorderDao.rejectClaim(id, reason.trim());
        auditLogService.log("理赔流程", "驳回", str(row, "order_no"), "工单#" + id + " 驳回原因：" + reason.trim());
        return done(id, "REJECTED");
    }

    private Map<String, Object> mustClaim(Long id) {
        Map<String, Object> row = workorderDao.claimById(id);
        if (row == null) {
            throw new IllegalArgumentException("理赔单不存在：" + id);
        }
        return row;
    }

    private void requireStatus(Map<String, Object> row, String expect, String op) {
        String st = str(row, "claim_status");
        if (!expect.equals(st)) {
            throw new IllegalArgumentException("工单 #" + row.get("id") + " 当前状态(" + st + ")不能执行「" + op + "」");
        }
    }

    private String str(Map<String, Object> row, String key) {
        return row.get(key) == null ? "" : String.valueOf(row.get(key));
    }

    private Map<String, Object> done(Long id, String status) {
        Map<String, Object> res = new HashMap<>(4);
        res.put("id", id);
        res.put("status", status);
        return res;
    }
}
