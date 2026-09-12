package com.java3y.austin.web.service.sentinel.tms;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 逆向售后退货闭环：登记退货 → 受理 → 退款 / 重发 → 关闭
 *
 * @author sentinel
 */
@Service
public class AfterSaleService {

    private final JdbcTemplate jdbcTemplate;
    private final AuditLogService auditLogService;

    public AfterSaleService(JdbcTemplate jdbcTemplate, AuditLogService auditLogService) {
        this.jdbcTemplate = jdbcTemplate;
        this.auditLogService = auditLogService;
    }

    public Map<String, Object> list(String orderNo, String status, int page, int perPage) {
        StringBuilder sql = new StringBuilder("SELECT * FROM after_sale WHERE is_deleted = 0");
        if (orderNo != null && !orderNo.isEmpty()) {
            sql.append(" AND order_no LIKE '%").append(orderNo).append("%'");
        }
        if (status != null && !status.isEmpty()) {
            sql.append(" AND status = '").append(status).append("'");
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

    /** 登记退货：从订单生成售后单，订单节点置为 RETURNED */
    public Map<String, Object> register(String orderNo, String reason, String type) {
        List<Map<String, Object>> orders = jdbcTemplate.queryForList(
                "SELECT * FROM logistics_order WHERE order_no = ? AND is_deleted = 0", orderNo);
        if (orders.isEmpty()) {
            throw new IllegalArgumentException("订单不存在：" + orderNo);
        }
        Map<String, Object> o = orders.get(0);
        String t = (type == null || type.isEmpty()) ? "RETURN" : type;
        String currency = o.get("declared_currency") != null ? String.valueOf(o.get("declared_currency")) : "CNY";
        jdbcTemplate.update(
                "INSERT INTO after_sale (order_no, merchant_id, type, reason, refund_amount, currency, status, buyer_name, buyer_address) "
                        + "VALUES (?,?,?,?,?,?,'PENDING',?,?)",
                orderNo, o.get("merchant_id"), t, reason,
                o.get("declared_value") != null ? o.get("declared_value") : 0,
                currency, o.get("buyer_id"), o.get("buyer_address"));
        jdbcTemplate.update("UPDATE logistics_order SET current_node = 'RETURNED' WHERE order_no = ?", orderNo);
        auditLogService.log("逆向售后", "登记退货", orderNo, "退货原因：" + (reason == null ? "" : reason));
        Map<String, Object> res = new HashMap<>();
        res.put("orderNo", orderNo);
        res.put("status", "PENDING");
        return res;
    }

    /** 受理并退款 */
    public Map<String, Object> refund(Long id) {
        Map<String, Object> s = find(id);
        String orderNo = String.valueOf(s.get("order_no"));
        jdbcTemplate.update("UPDATE after_sale SET status = 'REFUNDED' WHERE id = ?", id);
        jdbcTemplate.update("UPDATE logistics_order SET current_node = 'RETURNED' WHERE order_no = ?", orderNo);
        auditLogService.log("逆向售后", "退款", orderNo, "退款完成");
        Map<String, Object> res = new HashMap<>();
        res.put("id", id);
        res.put("status", "REFUNDED");
        return res;
    }

    /** 重发（换货） */
    public Map<String, Object> reship(Long id) {
        Map<String, Object> s = find(id);
        String orderNo = String.valueOf(s.get("order_no"));
        jdbcTemplate.update("UPDATE after_sale SET status = 'RESHIPPED' WHERE id = ?", id);
        jdbcTemplate.update("UPDATE logistics_order SET current_node = 'WAREHOUSE_OUT' WHERE order_no = ?", orderNo);
        auditLogService.log("逆向售后", "重发", orderNo, "换货重发出库");
        Map<String, Object> res = new HashMap<>();
        res.put("id", id);
        res.put("status", "RESHIPPED");
        return res;
    }

    /** 关闭售后 */
    public Map<String, Object> close(Long id) {
        Map<String, Object> s = find(id);
        String orderNo = String.valueOf(s.get("order_no"));
        jdbcTemplate.update("UPDATE after_sale SET status = 'CLOSED' WHERE id = ?", id);
        auditLogService.log("逆向售后", "关闭", orderNo, "售后单关闭");
        Map<String, Object> res = new HashMap<>();
        res.put("id", id);
        res.put("status", "CLOSED");
        return res;
    }

    public Map<String, Object> detail(Long id) {
        return find(id);
    }

    private Map<String, Object> find(Long id) {
        List<Map<String, Object>> list = jdbcTemplate.queryForList(
                "SELECT * FROM after_sale WHERE id = ? AND is_deleted = 0", id);
        if (list.isEmpty()) {
            throw new IllegalArgumentException("售后单不存在：" + id);
        }
        return list.get(0);
    }
}
