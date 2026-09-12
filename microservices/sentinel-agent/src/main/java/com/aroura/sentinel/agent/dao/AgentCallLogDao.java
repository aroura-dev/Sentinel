package com.aroura.sentinel.agent.dao;

import com.aroura.sentinel.agent.model.AgentCallLog;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Agent 调用日志 DAO（JdbcTemplate 实现，写入 agent_call_log 表）
 * <p>
 * 读取统一返回 snake_case 的 Map（SELECT *），与全站其它列表接口风格一致，
 * 避免 Bean 序列化（fastjson/Jackson）命名不一致导致前端 TraceId/Agent 列取不到值。
 *
 * @author sentinel
 */
@Repository
public class AgentCallLogDao {

    private final JdbcTemplate jdbcTemplate;

    public AgentCallLogDao(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void insert(AgentCallLog log) {
        jdbcTemplate.update(
                "INSERT INTO agent_call_log (agent_name, input, output, tools_called, token_usage, latency_ms, status, trace_id) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                log.getAgentName(), log.getInput(), log.getOutput(), log.getToolsCalled(),
                log.getTokenUsage(), log.getLatencyMs(), log.getStatus(), log.getTraceId());
    }

    public Map<String, Object> queryPage(String agentName, String traceId, String status, int page, int perPage) {
        StringBuilder where = new StringBuilder(" WHERE is_deleted = 0");
        Object[] args = new Object[3];
        int idx = 0;
        if (agentName != null && !agentName.trim().isEmpty()) {
            where.append(" AND agent_name LIKE ?");
            args[idx++] = "%" + agentName.trim() + "%";
        }
        if (traceId != null && !traceId.trim().isEmpty()) {
            where.append(" AND trace_id = ?");
            args[idx++] = traceId.trim();
        }
        if (status != null && !status.trim().isEmpty()) {
            where.append(" AND status = ?");
            args[idx++] = status.trim();
        }
        Object[] finalArgs = Arrays.copyOf(args, idx);
        String whereSql = where.toString();

        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM agent_call_log" + whereSql, Integer.class, finalArgs);
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT * FROM agent_call_log" + whereSql + " ORDER BY id DESC LIMIT ? OFFSET ?",
                new Object[]{perPage, (page - 1) * perPage});

        Map<String, Object> result = new HashMap<>(4);
        result.put("count", count == null ? 0 : count);
        result.put("rows", rows);
        return result;
    }

    public Map<String, Object> queryById(Long id) {
        List<Map<String, Object>> list = jdbcTemplate.queryForList(
                "SELECT * FROM agent_call_log WHERE id = ? AND is_deleted = 0", id);
        return list.isEmpty() ? null : list.get(0);
    }

    public List<Map<String, Object>> queryByTraceId(String traceId) {
        return jdbcTemplate.queryForList(
                "SELECT * FROM agent_call_log WHERE trace_id = ? AND is_deleted = 0 ORDER BY id ASC",
                traceId);
    }

    public Map<String, Object> stats() {
        Map<String, Object> result = new HashMap<>(8);
        result.put("agentCallCount", jdbcTemplate.queryForObject("SELECT COUNT(*) FROM agent_call_log WHERE is_deleted = 0", Integer.class));
        result.put("successCount", jdbcTemplate.queryForObject("SELECT COUNT(*) FROM agent_call_log WHERE status = 'success' AND is_deleted = 0", Integer.class));
        result.put("failedCount", jdbcTemplate.queryForObject("SELECT COUNT(*) FROM agent_call_log WHERE status = 'failed' AND is_deleted = 0", Integer.class));
        result.put("degradedCount", jdbcTemplate.queryForObject("SELECT COUNT(*) FROM agent_call_log WHERE status = 'degraded' AND is_deleted = 0", Integer.class));
        Double avgLatency = jdbcTemplate.queryForObject("SELECT AVG(latency_ms) FROM agent_call_log WHERE is_deleted = 0", Double.class);
        Double avgToken = jdbcTemplate.queryForObject("SELECT AVG(token_usage) FROM agent_call_log WHERE is_deleted = 0", Double.class);
        result.put("avgLatencyMs", avgLatency == null ? 0 : Math.round(avgLatency));
        result.put("avgTokenUsage", avgToken == null ? 0 : Math.round(avgToken));
        return result;
    }

    public String latestTraceChainId() {
        List<String> list = jdbcTemplate.queryForList(
                "SELECT trace_id FROM agent_call_log WHERE is_deleted = 0 "
                        + "AND trace_id IS NOT NULL AND trace_id <> '' "
                        + "GROUP BY trace_id HAVING COUNT(*) > 1 "
                        + "ORDER BY MAX(id) DESC LIMIT 1",
                String.class);
        return list.isEmpty() ? null : list.get(0);
    }
}