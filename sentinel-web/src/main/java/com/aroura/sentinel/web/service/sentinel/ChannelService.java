package com.aroura.sentinel.web.service.sentinel;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 渠道账号服务（复用 sentinel channel_account 表）
 * <p>
 * 从 SentinelChannelController 抽取，消除控制器内裸 SQL。
 *
 * @author sentinel
 */
@Service
public class ChannelService {

    private final JdbcTemplate jdbcTemplate;

    public ChannelService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Map<String, Object> list(int page, int perPage) {
        String where = " WHERE is_deleted = 0";
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM channel_account" + where, Integer.class);
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT id, name, send_channel, account_config, creator, created, updated FROM channel_account"
                        + where + " ORDER BY id DESC LIMIT ? OFFSET ?", perPage, (page - 1) * perPage);
        Map<String, Object> result = new HashMap<>(4);
        result.put("count", count == null ? 0 : count);
        result.put("rows", rows);
        return result;
    }

    public Long save(Map<String, Object> body) {
        String name = body.get("name") == null ? "未命名渠道" : String.valueOf(body.get("name"));
        Integer sendChannel = body.get("sendChannel") == null ? 40 : Integer.valueOf(String.valueOf(body.get("sendChannel")));
        String accountConfig = body.get("accountConfig") == null ? "{}" : String.valueOf(body.get("accountConfig"));
        String creator = body.get("creator") == null ? "sentinel" : String.valueOf(body.get("creator"));
        Object idObj = body.get("id");
        if (idObj != null) {
            Long id = Long.valueOf(String.valueOf(idObj));
            jdbcTemplate.update("UPDATE channel_account SET name=?, send_channel=?, account_config=? WHERE id=? AND is_deleted=0",
                    name, sendChannel, accountConfig, id);
            return id;
        }
        jdbcTemplate.update("INSERT INTO channel_account (name, send_channel, account_config, creator, created, updated) VALUES (?,?,?,?,UNIX_TIMESTAMP(),UNIX_TIMESTAMP())",
                name, sendChannel, accountConfig, creator);
        Number key = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Number.class);
        return key == null ? null : key.longValue();
    }

    public void delete(Long id) {
        jdbcTemplate.update("UPDATE channel_account SET is_deleted = 1 WHERE id = ?", id);
    }
}
