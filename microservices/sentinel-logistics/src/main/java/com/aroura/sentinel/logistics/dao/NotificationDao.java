package com.aroura.sentinel.logistics.dao;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
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
     * 落库并返回自增主键。
     * <p>
     * 原实现不返回主键，调用方只能再查一次「同 order_no 的最新一行」来定位刚插入的记录 ——
     * 那是个前导通配符的全表扫，且并发下可能取到别人的行、把状态改到错误的对象上。
     */
    public long insert(String orderNo, String node, String role, String channel, String content, String language, String status, String traceId) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(con -> {
            PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO notification_record (order_no, node, role, channel, content, language, status, trace_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, orderNo);
            ps.setString(2, node);
            ps.setString(3, role);
            ps.setString(4, channel);
            ps.setString(5, content);
            ps.setString(6, language);
            ps.setString(7, status);
            ps.setString(8, traceId);
            return ps;
        }, keyHolder);
        Number key = keyHolder.getKey();
        return key == null ? 0L : key.longValue();
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

    /** 投递成功：清空错误与重试时间。 */
    public void markSent(Long id) {
        jdbcTemplate.update(
                "UPDATE notification_record SET status = 'SENT', last_error = NULL, next_retry_at = NULL WHERE id = ? AND is_deleted = 0",
                id);
    }

    /** 无接收方/未启用投递：显式标记为跳过，避免与「卡住的 PENDING」混为一谈。 */
    public void markSkipped(Long id, String reason) {
        jdbcTemplate.update(
                "UPDATE notification_record SET status = 'SKIPPED', last_error = ?, next_retry_at = NULL WHERE id = ? AND is_deleted = 0",
                reason, id);
    }

    /**
     * 投递失败：登记失败原因、累计重试次数、设置退避后的下次重试时间。
     * retry_count 在 SQL 里自增，避免读改写竞态。
     */
    public void markFailed(Long id, String error, Timestamp nextRetryAt) {
        jdbcTemplate.update(
                "UPDATE notification_record SET status = 'FAILED', last_error = ?, next_retry_at = ?, retry_count = retry_count + 1 "
                        + "WHERE id = ? AND is_deleted = 0",
                error == null ? null : (error.length() > 255 ? error.substring(0, 255) : error), nextRetryAt, id);
    }

    /**
     * 原子认领一条待补偿记录，供多实例部署时互斥。
     * <p>
     * 补偿任务原先「先查后改」：两个实例会同时捞出同一批 FAILED 记录并各投一次，
     * 买家收到重复短信。这里把「领取」做成单条 UPDATE —— 只有把 next_retry_at
     * 推到未来且条件仍成立的那一个实例改动 1 行，其余返回 0 行，自然放弃。
     * <p>
     * 用租约（lease）而不是新状态：认领后若本实例崩溃，租约到期记录会重新可被领取，
     * 不需要额外的超时清理。租约需大于单次投递耗时（REST 超时 8s，取 60s 足够）。
     *
     * @return true 表示本次调用者成功认领，可以投递
     */
    public boolean claimForRetry(Long id, int maxAttempts, int leaseSeconds) {
        int updated = jdbcTemplate.update(
                "UPDATE notification_record SET next_retry_at = DATE_ADD(NOW(), INTERVAL ? SECOND) "
                        + "WHERE id = ? AND status = 'FAILED' AND retry_count < ? "
                        + "AND next_retry_at IS NOT NULL AND next_retry_at <= NOW() AND is_deleted = 0",
                leaseSeconds, id, maxAttempts);
        return updated == 1;
    }

    /**
     * 待补偿的记录：失败未达上限且已到退避时间。
     * 走 idx_retry(status, next_retry_at) 索引。
     */
    public List<Map<String, Object>> findRetryable(int maxAttempts, int limit) {
        return jdbcTemplate.queryForList(
                "SELECT * FROM notification_record WHERE status = 'FAILED' AND retry_count < ? "
                        + "AND next_retry_at IS NOT NULL AND next_retry_at <= NOW() AND is_deleted = 0 "
                        + "ORDER BY next_retry_at LIMIT ?",
                maxAttempts, limit);
    }

    /**
     * 回收卡住的 PENDING：进程在「落库」与「投递」之间崩溃会留下永久 PENDING。
     * 转为 FAILED 交给补偿路径重投（retry_count 不增，因为它从未真正投递过）。
     */
    public int reapStalePending(int staleMinutes, int limit) {
        return jdbcTemplate.update(
                "UPDATE notification_record SET status = 'FAILED', last_error = 'stale PENDING: 落库后未完成投递', "
                        + "next_retry_at = NOW() WHERE status = 'PENDING' AND is_deleted = 0 "
                        + "AND created_at < DATE_SUB(NOW(), INTERVAL ? MINUTE) LIMIT ?",
                staleMinutes, limit);
    }

    /**
     * 判断某订单在某节点、某角色近 N 小时内是否已通知过（用于 24h 去重）。
     * <p>
     * 只统计 SENT 与 PENDING：FAILED 代表没真正触达，SKIPPED 代表本就不该发 ——
     * 把它们计入「已通知」会让一次失败挡住之后 24 小时内的所有重发。
     */
    public boolean existsRecent(String orderNo, String node, String role, int hours) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM notification_record WHERE order_no = ? AND node = ? AND role = ? "
                        + "AND status IN ('SENT', 'PENDING') "
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