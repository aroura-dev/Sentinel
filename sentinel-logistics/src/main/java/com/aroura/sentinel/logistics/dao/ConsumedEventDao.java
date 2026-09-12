package com.aroura.sentinel.logistics.dao;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * 消费幂等 DAO（P0-2）
 * <p>
 * 基于 consumed_event 唯一键实现「同一事件只消费一次」。
 *
 * @author sentinel
 */
@Repository
public class ConsumedEventDao {

    private final JdbcTemplate jdbcTemplate;

    public ConsumedEventDao(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 首次消费返回 true；重复事件（唯一键冲突）返回 false
     */
    public boolean tryConsume(String eventId, String consumer) {
        int rows = jdbcTemplate.update(
                "INSERT IGNORE INTO consumed_event (event_id, consumer) VALUES (?, ?)",
                eventId, consumer);
        return rows > 0;
    }
}