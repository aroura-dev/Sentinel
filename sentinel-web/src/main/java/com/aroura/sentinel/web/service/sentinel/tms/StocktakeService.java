package com.aroura.sentinel.web.service.sentinel.tms;

import org.springframework.transaction.annotation.Transactional;
import com.aroura.sentinel.web.exception.CommonException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 移库盘点：新建盘点单 → 录入实盘 → 差异确认入账（DRAFT → DONE / CANCELED）
 *
 * @author sentinel
 */
@Service
public class StocktakeService {

    private final JdbcTemplate jdbcTemplate;
    private final InventoryService inventoryService;
    private final AuditLogService auditLogService;

    public StocktakeService(JdbcTemplate jdbcTemplate, InventoryService inventoryService,
                            AuditLogService auditLogService) {
        this.jdbcTemplate = jdbcTemplate;
        this.inventoryService = inventoryService;
        this.auditLogService = auditLogService;
    }

    public Map<String, Object> list(String status, int page, int perPage) {
        StringBuilder sql = new StringBuilder(
                "SELECT s.*, w.warehouse_name FROM stocktake s LEFT JOIN warehouse w ON w.id = s.warehouse_id AND w.is_deleted = 0 "
                        + "WHERE s.is_deleted = 0");
        List<Object> args = new ArrayList<>();
        if (status != null && !status.trim().isEmpty()) {
            // 原实现把 status 直接拼进字符串，既是注入口（' OR '1'='1 可绕过），
            // 也会让含单引号的正常输入直接报语法错
            sql.append(" AND s.status = ?");
            args.add(status.trim());
        }
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM (" + sql + ") t", Integer.class, args.toArray());
        List<Object> pageArgs = new ArrayList<>(args);
        pageArgs.add(perPage);
        pageArgs.add(Math.max((page - 1) * perPage, 0));
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                sql + " ORDER BY s.id DESC LIMIT ? OFFSET ?", pageArgs.toArray());
        Map<String, Object> res = new HashMap<>(4);
        res.put("count", count == null ? 0 : count);
        res.put("rows", rows);
        return res;
    }

    public Map<String, Object> detail(Long id) {
        Map<String, Object> head = findHead(id);
        Map<String, Object> result = new HashMap<>(4);
        result.put("stocktake", head);
        result.put("items", jdbcTemplate.queryForList(
                "SELECT * FROM stocktake_item WHERE stocktake_id = ? AND is_deleted = 0 ORDER BY sku", id));
        return result;
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> create(Long warehouseId, String scope, Long merchantId,
                                      String remark, List<String> skus) {
        if (warehouseId == null) {
            throw new CommonException("请选择仓库");
        }
        String sc = scope == null || scope.isEmpty() ? "ALL" : scope;
        String no = "STK" + new SimpleDateFormat("yyyyMMddHHmmssSSS").format(new Date());
        jdbcTemplate.update(
                "INSERT INTO stocktake (stocktake_no, warehouse_id, scope, merchant_id, remark, status, created_by, created_at) "
                        + "VALUES (?,?,?,?,?, 'DRAFT', ?, NOW())",
                no, warehouseId, sc, merchantId, remark, "admin");
        Long id = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        StringBuilder sql = new StringBuilder(
                "SELECT i.sku, p.name AS product_name, i.merchant_id, i.on_hand "
                        + "FROM inventory i LEFT JOIN product p ON p.sku = i.sku "
                        + "WHERE i.warehouse_id = ? AND i.is_deleted = 0");
        List<Object> args = new ArrayList<>();
        args.add(warehouseId);
        if ("MERCHANT".equals(sc) && merchantId != null) {
            sql.append(" AND i.merchant_id = ?");
            args.add(merchantId);
        }
        if ("MANUAL".equals(sc) && skus != null && !skus.isEmpty()) {
            StringBuilder in = new StringBuilder();
            for (int i = 0; i < skus.size(); i++) {
                in.append(i == 0 ? "?" : ",?");
                args.add(skus.get(i).trim());
            }
            sql.append(" AND i.sku IN (").append(in).append(")");
        }
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql.toString(), args.toArray());
        for (Map<String, Object> r : rows) {
            jdbcTemplate.update(
                    "INSERT INTO stocktake_item (stocktake_id, sku, product_name, merchant_id, expected, counted) VALUES (?,?,?,?,?, NULL)",
                    id, r.get("sku"), r.get("product_name"), r.get("merchant_id"), r.get("on_hand"));
        }
        auditLogService.log("移库盘点", "新建", no, "仓库=" + warehouseId + " 范围=" + sc + " 明细=" + rows.size());
        return detail(id);
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> saveCount(Long id, List<Map<String, Object>> counts) {
        Map<String, Object> head = findHead(id);
        if (!"DRAFT".equals(String.valueOf(head.get("status")))) {
            throw new CommonException("当前盘点单状态不允许录入实盘");
        }
        if (counts != null) {
            for (Map<String, Object> c : counts) {
                Object sku = c.get("sku");
                Object counted = c.get("counted");
                if (sku == null || counted == null) {
                    continue;
                }
                jdbcTemplate.update(
                        "UPDATE stocktake_item SET counted = ?, diff = IF(? IS NULL, NULL, ? - expected) "
                                + "WHERE stocktake_id = ? AND sku = ? AND is_deleted = 0",
                        counted, counted, counted, id, String.valueOf(sku));
            }
        }
        int total = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM stocktake_item WHERE stocktake_id = ? AND is_deleted = 0 AND counted IS NOT NULL",
                Integer.class, id);
        jdbcTemplate.update("UPDATE stocktake SET total_sku = ? WHERE id = ?", total, id);
        return detail(id);
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> finish(Long id) {
        Map<String, Object> head = findHead(id);
        if (!"DRAFT".equals(String.valueOf(head.get("status")))) {
            throw new CommonException("当前盘点单状态不允许完成");
        }
        Long warehouseId = ((Number) head.get("warehouse_id")).longValue();
        String no = String.valueOf(head.get("stocktake_no"));
        List<Map<String, Object>> items = jdbcTemplate.queryForList(
                "SELECT * FROM stocktake_item WHERE stocktake_id = ? AND is_deleted = 0 AND counted IS NOT NULL", id);
        if (items.isEmpty()) {
            throw new CommonException("请先录入实盘数量，再完成盘点");
        }
        int diffSku = 0;
        for (Map<String, Object> it : items) {
            int expected = ((Number) it.get("expected")).intValue();
            int counted = ((Number) it.get("counted")).intValue();
            int diff = counted - expected;
            jdbcTemplate.update("UPDATE stocktake_item SET diff = ? WHERE id = ?", diff, it.get("id"));
            if (diff != 0) {
                diffSku++;
                String sku = String.valueOf(it.get("sku"));
                if (diff > 0) {
                    inventoryService.adjust(sku, no, "STOCKTAKE_IN", diff, warehouseId);
                } else {
                    inventoryService.adjust(sku, no, "STOCKTAKE_OUT", -diff, warehouseId);
                }
            }
        }
        jdbcTemplate.update(
                "UPDATE stocktake SET status = 'DONE', total_sku = ?, diff_sku = ?, finished_at = NOW() WHERE id = ?",
                items.size(), diffSku, id);
        auditLogService.log("移库盘点", "完成", no,
                "已盘=" + items.size() + " 差异SKU=" + diffSku);
        return detail(id);
    }

    public Map<String, Object> cancel(Long id) {
        Map<String, Object> head = findHead(id);
        if (!"DRAFT".equals(String.valueOf(head.get("status")))) {
            throw new CommonException("当前盘点单状态不允许取消");
        }
        jdbcTemplate.update("UPDATE stocktake SET status = 'CANCELED' WHERE id = ?", id);
        return detail(id);
    }

    private Map<String, Object> findHead(Long id) {
        List<Map<String, Object>> list = jdbcTemplate.queryForList(
                "SELECT s.*, w.warehouse_name FROM stocktake s LEFT JOIN warehouse w ON w.id = s.warehouse_id "
                        + "WHERE s.id = ? AND s.is_deleted = 0", id);
        if (list.isEmpty()) {
            throw new CommonException("盘点单不存在：" + id);
        }
        return list.get(0);
    }
}