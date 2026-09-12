package com.java3y.austin.web.dao;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

/**
 * 角色/角色-菜单授权 DAO（role / role_menu）
 *
 * @author sentinel
 */
@Repository
public class RoleDao {

    private static final String COLUMNS = "id, code, name, description, status, created_at";

    private final JdbcTemplate jdbcTemplate;

    public RoleDao(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Map<String, Object>> findAll() {
        return jdbcTemplate.queryForList(
                "SELECT " + COLUMNS + " FROM role WHERE is_deleted = 0 ORDER BY id");
    }

    public Map<String, Object> findById(Long id) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT " + COLUMNS + " FROM role WHERE id = ? AND is_deleted = 0", id);
        return rows.isEmpty() ? null : rows.get(0);
    }

    public Map<String, Object> findByCode(String code) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT " + COLUMNS + " FROM role WHERE code = ? AND is_deleted = 0", code);
        return rows.isEmpty() ? null : rows.get(0);
    }

    public boolean existsByCode(String code) {
        Integer n = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM role WHERE code = ? AND is_deleted = 0", Integer.class, code);
        return n != null && n > 0;
    }

    public Long insert(String code, String name, String description) {
        jdbcTemplate.update(
                "INSERT INTO role (code, name, description) VALUES (?, ?, ?)",
                code, name, description);
        Number key = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Number.class);
        return key == null ? null : key.longValue();
    }

    public void update(Long id, String name, String description) {
        jdbcTemplate.update("UPDATE role SET name = ?, description = ? WHERE id = ? AND is_deleted = 0",
                name, description, id);
    }

    public void updateStatus(Long id, String status) {
        jdbcTemplate.update("UPDATE role SET status = ? WHERE id = ? AND is_deleted = 0", status, id);
    }

    /* ---------- role_menu ---------- */

    public List<String> findMenuPaths(String roleCode) {
        return jdbcTemplate.queryForList(
                "SELECT menu_path FROM role_menu WHERE role_code = ? ORDER BY id", String.class, roleCode);
    }

    public void saveMenus(String roleCode, List<String> paths) {
        jdbcTemplate.update("DELETE FROM role_menu WHERE role_code = ?", roleCode);
        if (paths != null && !paths.isEmpty()) {
            for (String p : paths) {
                jdbcTemplate.update(
                        "INSERT IGNORE INTO role_menu (role_code, menu_path) VALUES (?, ?)", roleCode, p);
            }
        }
    }
}
