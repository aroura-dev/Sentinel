package com.aroura.sentinel.logistics.dao;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

/**
 * 发件箱事件 DAO（P0-2 事件一致性）
 * <p>
 * 业务落库与事件写入同一事务；投递任务扫描 NEW 事件发送到 MQ。
 *
 * @author sentinel
 */
@Repository
public class OutboxEventDao {

    private final JdbcTemplate jdbcTemplate;

    public OutboxEventDao(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void insert(String eventId, String eventType, String aggregateType,
                       String aggregateId, String payload) {
        jdbcTemplate.update(
                "INSERT INTO outbox_event (event_id, event_type, aggregate_type, aggregate_id, payload) "
                        + "VALUES (?, ?, ?, ?, ?)",
                eventId, eventType, aggregateType, aggregateId, payload);
    }

    public List<Map<String, Object>> fetchPending(int limit) {
        return jdbcTemplate.queryForList(
                "SELECT id, event_id, event_type, aggregate_type, aggregate_id, payload, retry_count "
                        + "FROM outbox_event WHERE status = 'NEW' AND next_retry_at <= NOW() "
                        + "ORDER BY next_retry_at ASC, id ASC LIMIT ?",
                limit);
    }

    public int markSent(Long id) {
        return jdbcTemplate.update(
                "UPDATE outbox_event SET status = 'SENT', updated_at = NOW() "
                        + "WHERE id = ? AND status = 'NEW'", id);
    }

    public int markFailed(Long id, String error, int nextRetrySeconds, boolean dead) {
        String status = dead ? "DEAD" : "NEW";
        String err = error == null ? null : (error.length() > 500 ? error.substring(0, 500) : error);
        return jdbcTemplate.update(
                "UPDATE outbox_event SET status = ?, retry_count = retry_count + 1, last_error = ?, "
                        + "next_retry_at = DATE_ADD(NOW(), INTERVAL ? SECOND), updated_at = NOW() WHERE id = ?",
                status, err, nextRetrySeconds, id);
    }

    public Integer countByStatus(String status) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM outbox_event WHERE status = ?", Integer.class, status);
    }
}