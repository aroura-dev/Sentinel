package com.java3y.logistics.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * 订单查询（行级数据隔离）。
 * <p>
 * MERCHANT 角色只能看到「归属自己的商户」的订单：auth 已独立分库，无法跨库 JOIN sentinel_user，
 * 改为按 merchant.owner_username（冗余列，一次性回填）过滤；其余角色可见全部。
 *
 * @author sentinel-ms
 */
@Service
public class OrderQueryService {

    private static final String COLS =
            "o.id,o.order_no,o.merchant_id,o.current_node,o.sla_status,o.buyer_phone,o.destination_country";

    private final JdbcTemplate jdbcTemplate;

    public OrderQueryService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Map<String, Object> list(String username, String role, int page, int size) {
        StringBuilder where = new StringBuilder(" WHERE (o.is_deleted = 0 OR o.is_deleted IS NULL)");
        List<Object> args = new ArrayList<>();
        boolean scoped = false;
        if ("MERCHANT".equals(role) && username != null && !username.trim().isEmpty()) {
            where.append(" AND o.merchant_id IN (SELECT id FROM merchant WHERE owner_username = ? "
                    + "AND (is_deleted = 0 OR is_deleted IS NULL))");
            args.add(username.trim());
            scoped = true;
        }
        String base = " FROM logistics_order o" + where;
        Integer total = jdbcTemplate.queryForObject("SELECT COUNT(*)" + base, Integer.class, args.toArray());
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT " + COLS + base + " ORDER BY o.id DESC LIMIT ? OFFSET ?",
                concat(args, size, (page - 1) * size));

        Map<String, Object> resp = new HashMap<>(5);
        resp.put("total", total == null ? 0 : total);
        resp.put("rows", rows);
        resp.put("scoped", scoped);
        resp.put("scope_user", scoped ? username : null);
        return resp;
    }

    private Object[] concat(List<Object> head, Object... tail) {
        Object[] all = new Object[head.size() + tail.length];
        for (int i = 0; i < head.size(); i++) {
            all[i] = head.get(i);
        }
        System.arraycopy(tail, 0, all, head.size(), tail.length);
        return all;
    }
}
