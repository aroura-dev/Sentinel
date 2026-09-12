package com.java3y.austin.logistics.dao;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 异常知识库 DAO（落库 anomaly_knowledge）
 *
 * @author sentinel
 */
@Repository
public class KnowledgeDao {

    private final JdbcTemplate jdbcTemplate;

    public KnowledgeDao(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Map<String, Object> queryPage(String type, int page, int perPage) {
        StringBuilder where = new StringBuilder(" WHERE is_deleted = 0");
        List<Object> args = new ArrayList<>();
        if (type != null && !type.trim().isEmpty()) {
            where.append(" AND type LIKE ?");
            args.add("%" + type.trim() + "%");
        }
        String whereSql = where.toString();
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM anomaly_knowledge" + whereSql, Integer.class, args.toArray());
        List<Object> pageArgs = new ArrayList<>(args);
        pageArgs.add(perPage);
        pageArgs.add((page - 1) * perPage);
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT * FROM anomaly_knowledge" + whereSql + " ORDER BY id ASC LIMIT ? OFFSET ?", pageArgs.toArray());
        Map<String, Object> result = new HashMap<>(4);
        result.put("count", count == null ? 0 : count);
        result.put("rows", rows);
        return result;
    }

    public Map<String, Object> queryByStatusCode(String statusCode) {
        List<Map<String, Object>> list = jdbcTemplate.queryForList(
                "SELECT * FROM anomaly_knowledge WHERE status_code = ? AND is_deleted = 0", statusCode);
        return list.isEmpty() ? null : list.get(0);
    }

    /**
     * 新增或更新（按 status_code 唯一）
     */
    public void save(Long id, String statusCode, String type, String description, Integer avgDurationHours, String suggestion) {
        if (id != null) {
            jdbcTemplate.update(
                    "UPDATE anomaly_knowledge SET status_code = ?, type = ?, description = ?, avg_duration_hours = ?, suggestion = ? WHERE id = ? AND is_deleted = 0",
                    statusCode, type, description, avgDurationHours, suggestion, id);
            return;
        }
        jdbcTemplate.update(
                "INSERT INTO anomaly_knowledge (status_code, type, description, avg_duration_hours, suggestion) VALUES (?, ?, ?, ?, ?)",
                statusCode, type, description, avgDurationHours, suggestion);
    }

    public void deleteById(Long id) {
        jdbcTemplate.update("UPDATE anomaly_knowledge SET is_deleted = 1 WHERE id = ?", id);
    }
}