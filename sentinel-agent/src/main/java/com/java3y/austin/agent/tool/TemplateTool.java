package com.java3y.austin.agent.tool;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 通知模板查询工具（Tool Calling / 模板优先）
 * <p>
 * 复用 austin 的 message_template 表：按「物流节点 + 语言」匹配模板。
 * 约定模板命名规则：{@code sentinel:{node}:{language}}，例如 {@code sentinel:IMPORT_CUSTOMS:ru}。
 * 供 {@code ContentGenAgent} 实现「模板优先，Agent 兜底」。
 *
 * @author sentinel
 */
@Component
public class TemplateTool {

    private final JdbcTemplate jdbcTemplate;

    public TemplateTool(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Tool("按物流节点和语言从 message_template 表查询通知模板")
    public String queryTemplate(@P("物流节点 codeEn，如 IMPORT_CUSTOMS") String node,
                                @P("语言，如 ru/en/es/zh") String language) {
        String name = "sentinel:" + node + ":" + language;
        List<String> rows = jdbcTemplate.query(
                "SELECT msg_content FROM message_template WHERE name = ? AND is_deleted = 0 LIMIT 1",
                (rs, i) -> rs.getString("msg_content"), name);
        if (rows == null || rows.isEmpty()) {
            return null;
        }
        String content = rows.get(0);
        // msg_content 为 JSON 时提取纯文本 content 字段，否则原样返回
        if (content != null && content.trim().startsWith("{")) {
            try {
                JSONObject obj = JSON.parseObject(content);
                if (obj != null && obj.getString("content") != null) {
                    return obj.getString("content");
                }
            } catch (Exception ignored) {
                // 非法 JSON 按原样返回
            }
        }
        return content;
    }
}
