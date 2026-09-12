package com.aroura.sentinel.web.service.sentinel.tms;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 库存台账：SKU 库存 + 出入库流水（业务单据驱动）
 *
 * @author sentinel
 */
@Service
public class InventoryService {

    private final JdbcTemplate jdbcTemplate;
    private final AuditLogService auditLogService;

    public InventoryService(JdbcTemplate jdbcTemplate, AuditLogService auditLogService) {
        this.jdbcTemplate = jdbcTemplate;
        this.auditLogService = auditLogService;
    }

    public Map<String, Object> list(String sku, Long merchantId, int page, int perPage) {
        StringBuilder sql = new StringBuilder(
                "SELECT i.*, p.name AS product_name FROM inventory i LEFT JOIN product p ON i.sku = p.sku WHERE i.is_deleted = 0");
        if (sku != null && !sku.isEmpty()) {
            sql.append(" AND i.sku LIKE '%").append(sku).append("%'");
        }
        if (merchantId != null) {
            sql.append(" AND i.merchant_id = ").append(merchantId);
        }
        int count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM (" + sql + ") t", Integer.class);
        int offset = Math.max((page - 1) * perPage, 0);
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                sql + " ORDER BY i.updated_at DESC LIMIT " + offset + "," + perPage);
        Map<String, Object> res = new HashMap<>();
        res.put("count", count);
        res.put("rows", rows);
        return res;
    }

    public Map<String, Object> flow(String sku, int page, int perPage) {
        StringBuilder sql = new StringBuilder("SELECT * FROM inventory_flow WHERE is_deleted = 0");
        if (sku != null && !sku.isEmpty()) {
            sql.append(" AND sku LIKE '%").append(sku).append("%'");
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

    /** 出入库登记：更新库存并记流水（业务单据驱动，bizNo 为订单/运单号） */
    public Map<String, Object> adjust(String sku, String bizNo, String bizType, int qty, Long warehouseId) {
        Long wh = warehouseId != null ? warehouseId : 1L;
        List<Map<String, Object>> invs = jdbcTemplate.queryForList(
                "SELECT * FROM inventory WHERE sku = ? AND warehouse_id = ? AND is_deleted = 0", sku, wh);
        if (invs.isEmpty()) {
            // 不存在则基于商品建档
            jdbcTemplate.update(
                    "INSERT INTO inventory (sku, product_id, merchant_id, warehouse_id, on_hand, reserved) "
                            + "SELECT ?, id, merchant_id, ?, 0, 0 FROM product WHERE sku = ?",
                    sku, wh, sku);
        }
        boolean decrease = "OUT".equals(bizType) || "STOCKTAKE_OUT".equals(bizType);
        int delta = decrease ? -qty : qty;
        jdbcTemplate.update("UPDATE inventory SET on_hand = GREATEST(on_hand + ?, 0) WHERE sku = ? AND warehouse_id = ?",
                delta, sku, wh);
        jdbcTemplate.update(
                "INSERT INTO inventory_flow (sku, biz_no, biz_type, qty, warehouse_id) VALUES (?,?,?,?,?)",
                sku, bizNo, bizType, qty, wh);
        auditLogService.log("库存台账", "出入库登记", bizNo == null ? sku : bizNo,
                sku + " " + bizType + " x" + qty);
        Map<String, Object> res = new HashMap<>();
        res.put("sku", sku);
        res.put("bizType", bizType);
        res.put("qty", qty);
        return res;
    }
}
