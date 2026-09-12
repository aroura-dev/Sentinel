package com.aroura.sentinel.web.dao;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 用户 DAO（sentinel_user，多角色 RBAC）
 *
 * @author sentinel
 */
@Repository
public class SentinelUserDao {

    private static final String COLUMNS = "id, username, password, nickname, role, status, created_at";

    private final JdbcTemplate jdbcTemplate;

    public SentinelUserDao(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Map<String, Object> findByUsername(String username) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT " + COLUMNS + " FROM sentinel_user WHERE username = ? AND is_deleted = 0 LIMIT 1", username);
        return rows.isEmpty() ? null : rows.get(0);
    }

    public List<Map<String, Object>> listUsers() {
        return jdbcTemplate.queryForList(
                "SELECT id, username, nickname, role, status, created_at FROM sentinel_user WHERE is_deleted = 0 ORDER BY id");
    }

    public Map<String, Object> findById(Long id) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT id, username, nickname, role, status, created_at FROM sentinel_user WHERE id = ? AND is_deleted = 0", id);
        return rows.isEmpty() ? null : rows.get(0);
    }

    public boolean existsByUsername(String username) {
        Integer n = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sentinel_user WHERE username = ? AND is_deleted = 0", Integer.class, username);
        return n != null && n > 0;
    }

    public Map<String, Object> findPage(String keyword, String role, String status, int page, int perPage) {
        StringBuilder where = new StringBuilder(" WHERE is_deleted = 0");
        List<Object> args = new java.util.ArrayList<>();
        if (keyword != null && !keyword.trim().isEmpty()) {
            where.append(" AND (username LIKE ? OR nickname LIKE ?)");
            args.add("%" + keyword.trim() + "%");
            args.add("%" + keyword.trim() + "%");
        }
        if (role != null && !role.trim().isEmpty()) {
            where.append(" AND role = ?");
            args.add(role.trim());
        }
        if (status != null && !status.trim().isEmpty()) {
            where.append(" AND status = ?");
            args.add(status.trim());
        }
        String whereSql = where.toString();
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM sentinel_user" + whereSql, Integer.class, args.toArray());
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT id, username, nickname, role, status, created_at FROM sentinel_user" + whereSql
                        + " ORDER BY id DESC LIMIT ? OFFSET ?",
                java.util.stream.Stream.concat(args.stream(), java.util.stream.Stream.of(perPage, (page - 1) * perPage)).toArray());
        Map<String, Object> result = new HashMap<>(4);
        result.put("count", count == null ? 0 : count);
        result.put("rows", rows);
        return result;
    }

    public Long insert(String username, String passwordHash, String nickname, String role, String status) {
        jdbcTemplate.update(
                "INSERT INTO sentinel_user (username, password, nickname, role, status) VALUES (?, ?, ?, ?, ?)",
                username, passwordHash, nickname, role, status);
        Number key = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Number.class);
        return key == null ? null : key.longValue();
    }

    public void update(Long id, String nickname, String role, String status) {
        jdbcTemplate.update(
                "UPDATE sentinel_user SET nickname=?, role=?, status=? WHERE id=? AND is_deleted=0",
                nickname, role, status, id);
    }

    public void updateStatus(Long id, String status) {
        jdbcTemplate.update("UPDATE sentinel_user SET status=? WHERE id=? AND is_deleted=0", status, id);
    }

    public void updatePassword(Long id, String passwordHash) {
        jdbcTemplate.update("UPDATE sentinel_user SET password=? WHERE id=? AND is_deleted=0", passwordHash, id);
    }

    /** 除指定用户外，当前启用中的 ADMIN 数量（用于“至少保留一个管理员”保护） */
    public int countEnabledAdminExcluding(Long id) {
        Integer n = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sentinel_user WHERE role = 'ADMIN' AND status = '1' AND is_deleted = 0 AND id <> ?",
                Integer.class, id);
        return n == null ? 0 : n;
    }
}
