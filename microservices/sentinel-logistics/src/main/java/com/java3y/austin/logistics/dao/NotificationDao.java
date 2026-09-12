package com.java3y.austin.logistics.dao;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 通知记录 DAO（落库 notification_record）
 *
 * @author sentinel
 */
@Repository
public class NotificationDao {

    private final JdbcTemplate jdbcTemplate;

    public NotificationDao(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void insert(String orderNo, String node, String role, String channel, String content, String language, String status, String traceId) {
        jdbcTemplate.update(
                "INSERT INTO notification_record (order_no, node, role, channel, content, language, status, trace_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                orderNo, node, role, channel, content, language, status, traceId);
    }

    public Map<String, Object> queryPage(String orderNo, String channel, String status, int page, int perPage) {
        StringBuilder where = new StringBuilder(" WHERE is_deleted = 0");
        List<Object> args = new ArrayList<>();
        if (orderNo != null && !orderNo.trim().isEmpty()) {
            where.append(" AND order_no LIKE ?");
            args.add("%" + orderNo.trim() + "%");
        }
        if (channel != null && !channel.trim().isEmpty()) {
            where.append(" AND channel = ?");
            args.add(channel.trim());
        }
        if (status != null && !status.trim().isEmpty()) {
            where.append(" AND status = ?");
            args.add(status.trim());
        }
        String whereSql = where.toString();
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM notification_record" + whereSql, Integer.class, args.toArray());
        List<Object> pageArgs = new ArrayList<>(args);
        pageArgs.add(perPage);
        pageArgs.add((page - 1) * perPage);
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT * FROM notification_record" + whereSql + " ORDER BY id DESC LIMIT ? OFFSET ?", pageArgs.toArray());
        Map<String, Object> result = new HashMap<>(4);
        result.put("count", count == null ? 0 : count);
        result.put("rows", rows);
        return result;
    }

    public Map<String, Object> queryById(Long id) {
        List<Map<String, Object>> list = jdbcTemplate.queryForList(
                "SELECT * FROM notification_record WHERE id = ? AND is_deleted = 0", id);
        return list.isEmpty() ? null : list.get(0);
    }

    public Map<String, Object> stats() {
        Map<String, Object> result = new HashMap<>(8);
        Integer todayCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM notification_record WHERE is_deleted = 0 AND created_at >= CURDATE()", Integer.class);
        Integer total = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM notification_record WHERE is_deleted = 0", Integer.class);
        Integer success = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM notification_record WHERE is_deleted = 0 AND status IN ('SENT','SUCCESS')", Integer.class);
        Integer channels = jdbcTemplate.queryForObject(
                "SELECT COUNT(DISTINCT channel) FROM notification_record WHERE is_deleted = 0 AND channel IS NOT NULL AND channel != ''", Integer.class);
        Integer activeOrders = jdbcTemplate.queryForObject(
                "SELECT COUNT(DISTINCT order_no) FROM notification_record WHERE is_deleted = 0 AND created_at >= DATE_SUB(NOW(), INTERVAL 7 DAY)", Integer.class);
        double rate = (total == null || total == 0) ? 0 : (success == null ? 0 : success * 100.0 / total);
        result.put("todayCount", todayCount == null ? 0 : todayCount);
        result.put("successRate", Math.round(rate * 10) / 10.0);
        result.put("totalChannels", channels == null ? 0 : channels);
        result.put("activeOrders", activeOrders == null ? 0 : activeOrders);
        return result;
    }

    public void updateStatus(Long id, String status) {
        jdbcTemplate.update("UPDATE notification_record SET status = ? WHERE id = ? AND is_deleted = 0", status, id);
    }

    /**
     * 判断某订单在某节点、某角色近 N 小时内是否已发送过通知（用于 24h 去重）
     */
    public boolean existsRecent(String orderNo, String node, String role, int hours) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM notification_record WHERE order_no = ? AND node = ? AND role = ? "
                        + "AND is_deleted = 0 AND created_at > DATE_SUB(NOW(), INTERVAL " + hours + " HOUR)",
                Integer.class, orderNo, node, role);
        return count != null && count > 0;
    }

    /**
     * 统计某买家近 N 小时内收到的通知数（用于退订预测频次输入）
     */
    public int countRecent(String buyerId, int hours) {
        // buyer_id 未在 notification_record 表冗余存储，此处按订单归属统计：
        // 通过 order_no 关联 logistics_order.buyer_id
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM notification_record nr "
                        + "JOIN logistics_order lo ON nr.order_no = lo.order_no "
                        + "WHERE lo.buyer_id = ? AND nr.is_deleted = 0 "
                        + "AND nr.created_at > DATE_SUB(NOW(), INTERVAL " + hours + " HOUR)",
                Integer.class, buyerId);
        return count == null ? 0 : count;
    }
}