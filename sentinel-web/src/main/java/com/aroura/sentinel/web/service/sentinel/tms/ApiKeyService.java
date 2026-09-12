package com.aroura.sentinel.web.service.sentinel.tms;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 开放 API 应用凭证管理：创建 / 启停 / 吊销
 *
 * <p>安全约束：Secret 仅在创建时返回一次，列表 / 启停等后续接口一律脱敏。
 *
 * @author sentinel
 */
@Service
public class ApiKeyService {

    private final JdbcTemplate jdbcTemplate;
    private final AuditLogService auditLogService;

    public ApiKeyService(JdbcTemplate jdbcTemplate, AuditLogService auditLogService) {
        this.jdbcTemplate = jdbcTemplate;
        this.auditLogService = auditLogService;
    }

    public List<Map<String, Object>> list() {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT * FROM api_key WHERE is_deleted = 0 ORDER BY id DESC");
        rows.forEach(row -> row.remove("secret"));
        return rows;
    }

    public Map<String, Object> create(String appName, String company, String contactName,
                                      String contactPhone, String contactEmail, String scope,
                                      String remark, String createdBy) {
        String key = "sk_" + UUID.randomUUID().toString().replace("-", "").substring(0, 24);
        String secret = UUID.randomUUID().toString().replace("-", "").substring(0, 32);
        jdbcTemplate.update(
                "INSERT INTO api_key (app_name, company, contact_name, contact_phone, contact_email, "
                        + "api_key, secret, scope, remark, status, created_by) "
                        + "VALUES (?,?,?,?,?,?,?,?,?,1,?)",
                trimToNull(appName), trimToNull(company), trimToNull(contactName),
                trimToNull(contactPhone), trimToNull(contactEmail), key, secret,
                trimToNull(scope) == null ? "order:read" : scope, trimToNull(remark), createdBy);
        Long id = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        auditLogService.log("开放API", "创建应用凭证", appName, "scope:" + scope);
        Map<String, Object> res = find(id);
        res.put("api_key", key);
        res.put("secret", secret);
        return res;
    }

    public Map<String, Object> toggle(Long id) {
        Map<String, Object> k = find(id);
        int current = Integer.parseInt(String.valueOf(k.get("status")));
        int next = current == 1 ? 0 : 1;
        jdbcTemplate.update("UPDATE api_key SET status = ? WHERE id = ?", next, id);
        auditLogService.log("开放API", next == 1 ? "启用" : "停用", String.valueOf(k.get("app_name")), "凭证ID:" + id);
        Map<String, Object> res = find(id);
        res.remove("secret");
        return res;
    }

    public void delete(Long id) {
        jdbcTemplate.update("UPDATE api_key SET is_deleted = 1 WHERE id = ?", id);
        auditLogService.log("开放API", "吊销凭证", String.valueOf(id), "");
    }

    private static String trimToNull(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private Map<String, Object> find(Long id) {
        List<Map<String, Object>> list = jdbcTemplate.queryForList(
                "SELECT * FROM api_key WHERE id = ? AND is_deleted = 0", id);
        if (list.isEmpty()) {
            throw new IllegalArgumentException("凭证不存在：" + id);
        }
        return list.get(0);
    }
}