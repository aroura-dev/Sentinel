package com.aroura.sentinel.web.service.sentinel.tms;

import org.springframework.transaction.annotation.Transactional;
import com.aroura.sentinel.web.support.TenantScopeResolver;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
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

    private final TenantScopeResolver tenantScope;

    public AfterSaleService(JdbcTemplate jdbcTemplate, AuditLogService auditLogService,
                            TenantScopeResolver tenantScope) {
        this.jdbcTemplate = jdbcTemplate;
        this.auditLogService = auditLogService;
        this.tenantScope = tenantScope;
    }

    /**
     * 售后单分页。
     *
     * @param merchantScope 为 null 表示不限制（平台角色 / 后台线程），非 null 则限定该商家
     */
    public Map<String, Object> list(String orderNo, String status, Long merchantScope, int page, int perPage) {
        StringBuilder sql = new StringBuilder("SELECT * FROM after_sale WHERE is_deleted = 0");
        List<Object> args = new ArrayList<>();
        if (orderNo != null && !orderNo.trim().isEmpty()) {
            sql.append(" AND order_no LIKE ?");
            args.add("%" + orderNo.trim() + "%");
        }
        if (status != null && !status.trim().isEmpty()) {
            sql.append(" AND status = ?");
            args.add(status.trim());
        }
        if (merchantScope != null) {
            sql.append(" AND merchant_id = ?");
            args.add(merchantScope);
        }
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM (" + sql + ") t", Integer.class, args.toArray());
        List<Object> pageArgs = new ArrayList<>(args);
        pageArgs.add(perPage);
        pageArgs.add(Math.max((page - 1) * perPage, 0));
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                sql + " ORDER BY id DESC LIMIT ? OFFSET ?", pageArgs.toArray());
        Map<String, Object> res = new HashMap<>();
        res.put("count", count == null ? 0 : count);
        res.put("rows", rows);
        return res;
    }

    /** 登记退货：从订单生成售后单，订单节点置为 RETURNED */
    @Transactional(rollbackFor = Exception.class)
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
    @Transactional(rollbackFor = Exception.class)
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
    @Transactional(rollbackFor = Exception.class)
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
    @Transactional(rollbackFor = Exception.class)
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
        Map<String, Object> row = find(id);
        tenantScope.assertAccessible(merchantIdOf(row), "售后单");
        return row;
    }

    private static Long merchantIdOf(Map<String, Object> row) {
        if (row == null || row.get("merchant_id") == null) {
            return null;
        }
        return Long.valueOf(String.valueOf(row.get("merchant_id")));
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
