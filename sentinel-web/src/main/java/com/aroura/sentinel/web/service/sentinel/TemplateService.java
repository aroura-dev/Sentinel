package com.aroura.sentinel.web.service.sentinel;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 通知模板服务（复用 sentinel message_template 表）
 * <p>
 * 从 SentinelTemplateController 抽取，消除控制器内裸 SQL。
 *
 * @author sentinel
 */
@Service
public class TemplateService {

    private final JdbcTemplate jdbcTemplate;

    public TemplateService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Map<String, Object> list(String keywords, int page, int perPage) {
        String where = " WHERE is_deleted = 0";
        List<Object> args = new ArrayList<>();
        if (keywords != null && !keywords.trim().isEmpty()) {
            where += " AND (name LIKE ? OR msg_content LIKE ?)";
            args.add("%" + keywords.trim() + "%");
            args.add("%" + keywords.trim() + "%");
        }
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM message_template" + where, Integer.class, args.toArray());
        List<Object> pageArgs = new ArrayList<>(args);
        pageArgs.add(perPage);
        pageArgs.add((page - 1) * perPage);
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT id, name, audit_status, msg_status, send_channel, template_type, msg_type, msg_content, creator, created, updated FROM message_template"
                        + where + " ORDER BY id DESC LIMIT ? OFFSET ?", pageArgs.toArray());
        Map<String, Object> result = new HashMap<>(4);
        result.put("count", count == null ? 0 : count);
        result.put("rows", rows);
        return result;
    }

    public Long save(Map<String, Object> body) {
        String name = str(body.get("name"), "未命名模板");
        String msgContent = str(body.get("msgContent"), "");
        Integer sendChannel = intOr(body.get("sendChannel"), 40);
        Integer templateType = intOr(body.get("templateType"), 10);
        Integer msgType = intOr(body.get("msgType"), 10);
        Integer auditStatus = intOr(body.get("auditStatus"), 10);
        Integer msgStatus = intOr(body.get("msgStatus"), 10);
        Integer idType = intOr(body.get("idType"), 10);
        Integer sendAccount = intOr(body.get("sendAccount"), 0);
        String creator = str(body.get("creator"), "sentinel");

        Object idObj = body.get("id");
        if (idObj != null) {
            Long id = Long.valueOf(String.valueOf(idObj));
            jdbcTemplate.update(
                    "UPDATE message_template SET name=?, msg_content=?, send_channel=?, template_type=?, msg_type=?, audit_status=?, msg_status=?, id_type=?, send_account=? WHERE id=? AND is_deleted=0",
                    name, msgContent, sendChannel, templateType, msgType, auditStatus, msgStatus, idType, sendAccount, id);
            return id;
        }
        jdbcTemplate.update(
                "INSERT INTO message_template (name, audit_status, msg_status, id_type, send_channel, template_type, msg_type, msg_content, send_account, creator, updator, auditor, team, proposer) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                name, auditStatus, msgStatus, idType, sendChannel, templateType, msgType, msgContent, sendAccount,
                creator, creator, creator, "sentinel", creator);
        Number key = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Number.class);
        return key == null ? null : key.longValue();
    }

    public void delete(Long id) {
        jdbcTemplate.update("UPDATE message_template SET is_deleted = 1 WHERE id = ?", id);
    }

    private static String str(Object o, String dft) {
        return o == null ? dft : String.valueOf(o);
    }

    private static Integer intOr(Object o, Integer dft) {
        if (o == null) {
            return dft;
        }
        try {
            return Integer.valueOf(String.valueOf(o));
        } catch (NumberFormatException e) {
            return dft;
        }
    }
}
