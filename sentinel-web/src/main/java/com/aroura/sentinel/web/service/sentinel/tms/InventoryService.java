package com.aroura.sentinel.web.service.sentinel.tms;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
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

    /**
     * 库存台账分页。
     *
     * @param merchantScope 为 null 表示不限制（平台角色 / 后台线程），非 null 则限定该商家
     */
    public Map<String, Object> list(String sku, Long merchantScope, int page, int perPage) {
        StringBuilder sql = new StringBuilder(
                "SELECT i.*, p.name AS product_name FROM inventory i LEFT JOIN product p ON i.sku = p.sku WHERE i.is_deleted = 0");
        List<Object> args = new ArrayList<>();
        if (sku != null && !sku.trim().isEmpty()) {
            sql.append(" AND i.sku LIKE ?");
            args.add("%" + sku.trim() + "%");
        }
        if (merchantScope != null) {
            sql.append(" AND i.merchant_id = ?");
            args.add(merchantScope);
        }
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM (" + sql + ") t", Integer.class, args.toArray());
        List<Object> pageArgs = new ArrayList<>(args);
        pageArgs.add(perPage);
        pageArgs.add(Math.max((page - 1) * perPage, 0));
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                sql + " ORDER BY i.updated_at DESC LIMIT ? OFFSET ?", pageArgs.toArray());
        Map<String, Object> res = new HashMap<>();
        res.put("count", count == null ? 0 : count);
        res.put("rows", rows);
        return res;
    }

    /**
     * 出入库流水分页。
     * <p>
     * {@code inventory_flow} 表本身没有 {@code merchant_id}，只能经 {@code (sku, warehouse_id)}
     * 关联 inventory 判定归属。<b>不能用 sku 单列关联</b> —— product 的唯一键是
     * {@code uk_merchant_sku (merchant_id, sku)}，SKU 并非全局唯一，两家用同一 SKU 编码时
     * 会互相看到对方的流水。{@code warehouse_id} 为 NULL 的历史行将永不匹配（fail-closed）。
     *
     * @param merchantScope 为 null 表示不限制
     */
    public Map<String, Object> flow(String sku, Long merchantScope, int page, int perPage) {
        StringBuilder sql = new StringBuilder("SELECT f.* FROM inventory_flow f WHERE f.is_deleted = 0");
        List<Object> args = new ArrayList<>();
        if (sku != null && !sku.trim().isEmpty()) {
            sql.append(" AND f.sku LIKE ?");
            args.add("%" + sku.trim() + "%");
        }
        if (merchantScope != null) {
            sql.append(" AND EXISTS (SELECT 1 FROM inventory i WHERE i.sku = f.sku"
                    + " AND i.warehouse_id = f.warehouse_id AND i.merchant_id = ? AND i.is_deleted = 0)");
            args.add(merchantScope);
        }
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM (" + sql + ") t", Integer.class, args.toArray());
        List<Object> pageArgs = new ArrayList<>(args);
        pageArgs.add(perPage);
        pageArgs.add(Math.max((page - 1) * perPage, 0));
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                sql + " ORDER BY f.id DESC LIMIT ? OFFSET ?", pageArgs.toArray());
        Map<String, Object> res = new HashMap<>();
        res.put("count", count == null ? 0 : count);
        res.put("rows", rows);
        return res;
    }

    /** 出入库登记：更新库存并记流水（业务单据驱动，bizNo 为订单/运单号） */
    @Transactional(rollbackFor = Exception.class)
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
