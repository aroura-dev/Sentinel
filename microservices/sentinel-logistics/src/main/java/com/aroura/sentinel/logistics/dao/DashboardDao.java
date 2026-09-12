package com.aroura.sentinel.logistics.dao;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 看板统计 DAO（图表数据，基于真实业务表）
 *
 * @author sentinel
 */
@Repository
public class DashboardDao {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("MM-dd");

    private final JdbcTemplate jdbcTemplate;

    public DashboardDao(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 近 7 日通知渠道分布 [{name, value}]
     */
    public List<Map<String, Object>> channelDistribution() {
        return jdbcTemplate.queryForList(
                "SELECT channel AS name, COUNT(*) AS value FROM notification_record "
                        + "WHERE is_deleted = 0 AND channel IS NOT NULL AND channel != '' "
                        + "AND created_at >= DATE_SUB(NOW(), INTERVAL 7 DAY) "
                        + "GROUP BY channel ORDER BY value DESC");
    }

    /**
     * 近 7 日 Agent 调用趋势 {dates, success, failed}
     */
    public Map<String, Object> agentTrend() {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT DATE_FORMAT(created_at, '%m-%d') AS d, status, COUNT(*) AS c FROM agent_call_log "
                        + "WHERE is_deleted = 0 AND created_at >= DATE_SUB(NOW(), INTERVAL 7 DAY) "
                        + "GROUP BY DATE_FORMAT(created_at, '%m-%d'), status ORDER BY d");
        Map<String, Integer> successMap = new HashMap<>();
        Map<String, Integer> failedMap = new HashMap<>();
        for (Map<String, Object> row : rows) {
            String d = String.valueOf(row.get("d"));
            String status = String.valueOf(row.get("status"));
            Integer c = row.get("c") == null ? 0 : ((Number) row.get("c")).intValue();
            if ("success".equals(status)) {
                successMap.put(d, c);
            } else {
                failedMap.put(d, c);
            }
        }
        List<String> dates = new ArrayList<>();
        List<Integer> success = new ArrayList<>();
        List<Integer> failed = new ArrayList<>();
        LocalDate today = LocalDate.now();
        for (int i = 6; i >= 0; i--) {
            String d = today.minusDays(i).format(FMT);
            dates.add(d);
            success.add(successMap.getOrDefault(d, 0));
            failed.add(failedMap.getOrDefault(d, 0));
        }
        Map<String, Object> result = new HashMap<>(4);
        result.put("dates", dates);
        result.put("success", success);
        result.put("failed", failed);
        return result;
    }
}