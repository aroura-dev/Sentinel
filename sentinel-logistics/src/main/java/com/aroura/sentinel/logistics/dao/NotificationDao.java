package com.aroura.sentinel.logistics.dao;

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

    /**
     * 插入通知记录。
     *
     * @return 新记录主键。调用方应持有它来回写投递结果 —— 此前返回 void，导致调用方只能靠
     *         {@code (orderNo, channel)} 反查"最新一条"，同一订单同一渠道并发时会拿到别人的行。
     */
    public Long insert(String orderNo, String node, String role, String channel, String content, String language, String status, String traceId) {
        jdbcTemplate.update(
                "INSERT INTO notification_record (order_no, node, role, channel, content, language, status, trace_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                orderNo, node, role, channel, content, language, status, traceId);
        Number key = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Number.class);
        return key == null ? null : key.longValue();
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
        // 成功率的分母只算"真正尝试过投递"的记录：PENDING 表示尚未投递、
        // SKIPPED 表示分发未启用或缺少买家标识而根本不会投递，二者计入分母会把分子稀释掉
        Integer attempted = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM notification_record WHERE is_deleted = 0 AND status NOT IN ('PENDING','SKIPPED')", Integer.class);
        Integer channels = jdbcTemplate.queryForObject(
                "SELECT COUNT(DISTINCT channel) FROM notification_record WHERE is_deleted = 0 AND channel IS NOT NULL AND channel != ''", Integer.class);
        Integer activeOrders = jdbcTemplate.queryForObject(
                "SELECT COUNT(DISTINCT order_no) FROM notification_record WHERE is_deleted = 0 AND created_at >= DATE_SUB(NOW(), INTERVAL 7 DAY)", Integer.class);
        double rate = (attempted == null || attempted == 0) ? 0 : (success == null ? 0 : success * 100.0 / attempted);
        result.put("todayCount", todayCount == null ? 0 : todayCount);
        result.put("successRate", Math.round(rate * 10) / 10.0);
        result.put("totalChannels", channels == null ? 0 : channels);
        result.put("activeOrders", activeOrders == null ? 0 : activeOrders);
        result.put("totalCount", total == null ? 0 : total);
        result.put("attemptedCount", attempted == null ? 0 : attempted);
        return result;
    }

    public void updateStatus(Long id, String status) {
        jdbcTemplate.update("UPDATE notification_record SET status = ? WHERE id = ? AND is_deleted = 0", status, id);
    }

    /**
     * 按 traceId 回写投递结果。供发送责任链使用 —— 责任链各组件拿到的只有 TaskInfo.bizId
     * （即插入时写入的 trace_id），没有记录主键。
     *
     * @return 受影响行数；0 表示责任链跑的这条消息本来就没有对应通知记录（正常情况）
     */
    public int updateStatusByTraceId(String traceId, String status) {
        if (traceId == null || traceId.trim().isEmpty()) {
            return 0;
        }
        return jdbcTemplate.update(
                "UPDATE notification_record SET status = ? WHERE trace_id = ? AND is_deleted = 0",
                status, traceId.trim());
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