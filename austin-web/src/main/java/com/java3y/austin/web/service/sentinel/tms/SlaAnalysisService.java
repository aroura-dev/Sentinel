package com.java3y.austin.web.service.sentinel.tms;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 时效分析服务：运单准时率 / 渠道 / 承运商 / 延误原因聚合
 * <p>准时 = 已妥投且实际送达时间 ≤ 承诺 ETA；平均时效 = 出库到妥投的小时数。
 * <p>支持按运单/订单创建时间范围（start ~ end，含端点当天）筛选。
 *
 * @author sentinel
 */
@Service
public class SlaAnalysisService {

    private final JdbcTemplate jdbcTemplate;

    public SlaAnalysisService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Map<String, Object> overview(String start, String end) {
        Map<String, Object> result = new HashMap<>(8);
        Range wc = range("w", start, end);
        Range oc = range("o", start, end);
        int total = cnt("SELECT COUNT(*) FROM waybill w WHERE w.is_deleted = 0" + wc.sql, wc.args);
        int delivered = cnt("SELECT COUNT(*) FROM waybill w WHERE w.is_deleted = 0 AND w.status = 'DELIVERED'" + wc.sql, wc.args);
        int onTime = cnt("SELECT COUNT(*) FROM waybill w WHERE w.is_deleted = 0 AND w.status = 'DELIVERED' "
                + "AND w.actual_delivered_at IS NOT NULL AND w.actual_delivered_at <= w.promise_eta" + wc.sql, wc.args);
        result.put("total", total);
        result.put("delivered", delivered);
        result.put("onTime", onTime);
        result.put("onTimeRate", delivered > 0 ? scale(onTime * 100.0 / delivered) : BigDecimal.ZERO);
        result.put("avgTransitHours", avg("SELECT COALESCE(AVG(TIMESTAMPDIFF(HOUR, w.created_at, w.actual_delivered_at)),0) "
                + "FROM waybill w WHERE w.is_deleted = 0 AND w.status = 'DELIVERED' AND w.actual_delivered_at IS NOT NULL" + wc.sql, wc.args));
        result.put("inTransit", cnt("SELECT COUNT(*) FROM logistics_order o JOIN waybill w ON o.waybill_no = w.waybill_no "
                + "WHERE o.is_deleted = 0 AND w.is_deleted = 0 "
                + "AND o.current_node NOT IN ('DELIVERED','LOST','RETURNED')" + wc.sql, wc.args));
        result.put("anomalyCount", cnt("SELECT COUNT(*) FROM logistics_order o WHERE o.is_deleted = 0 "
                + "AND o.current_node IN ('CUSTOMS_DELAY','DELIVERY_FAILED','LOST','RETURNED')" + oc.sql, oc.args));
        return result;
    }

    public List<Map<String, Object>> byChannel(String start, String end) {
        Range wc = range("w", start, end);
        return jdbcTemplate.queryForList(
                "SELECT c.channel_code, c.channel_name, COUNT(w.id) AS total, "
                        + "SUM(CASE WHEN w.status='DELIVERED' THEN 1 ELSE 0 END) AS delivered, "
                        + "SUM(CASE WHEN w.status='DELIVERED' AND w.actual_delivered_at IS NOT NULL AND w.actual_delivered_at <= w.promise_eta THEN 1 ELSE 0 END) AS onTime, "
                        + "COALESCE(AVG(CASE WHEN w.status='DELIVERED' AND w.actual_delivered_at IS NOT NULL "
                        + "THEN TIMESTAMPDIFF(HOUR, w.created_at, w.actual_delivered_at) END),0) AS avgTransitHours "
                        + "FROM waybill w JOIN carrier_channel c ON w.channel_id = c.id "
                        + "WHERE w.is_deleted = 0 AND w.channel_id IS NOT NULL" + wc.sql
                        + " GROUP BY c.channel_code, c.channel_name ORDER BY total DESC",
                wc.args.toArray());
    }

    public List<Map<String, Object>> byCarrier(String start, String end) {
        Range wc = range("w", start, end);
        return jdbcTemplate.queryForList(
                "SELECT ca.id AS carrier_id, ca.carrier_code, ca.carrier_name, COUNT(w.id) AS total, "
                        + "SUM(CASE WHEN w.status='DELIVERED' THEN 1 ELSE 0 END) AS delivered, "
                        + "SUM(CASE WHEN w.status='DELIVERED' AND w.actual_delivered_at IS NOT NULL AND w.actual_delivered_at <= w.promise_eta THEN 1 ELSE 0 END) AS onTime, "
                        + "COALESCE(AVG(CASE WHEN w.status='DELIVERED' AND w.actual_delivered_at IS NOT NULL "
                        + "THEN TIMESTAMPDIFF(HOUR, w.created_at, w.actual_delivered_at) END),0) AS avgTransitHours, "
                        + "(SELECT COUNT(*) FROM logistics_order o WHERE o.carrier_id = ca.id AND o.is_deleted = 0 "
                        + "AND o.current_node IN ('CUSTOMS_DELAY','DELIVERY_FAILED','LOST','RETURNED')) AS anomalyCount "
                        + "FROM waybill w JOIN carrier ca ON w.carrier_id = ca.id "
                        + "WHERE w.is_deleted = 0 AND w.carrier_id IS NOT NULL" + wc.sql
                        + " GROUP BY ca.id, ca.carrier_code, ca.carrier_name ORDER BY total DESC",
                wc.args.toArray());
    }

    public List<Map<String, Object>> topDelay(String start, String end) {
        Range wc = range("wo", start, end);
        return jdbcTemplate.queryForList(
                "SELECT wo.type, COUNT(*) AS cnt FROM workorder wo WHERE wo.is_deleted = 0" + wc.sql
                        + " GROUP BY wo.type ORDER BY cnt DESC",
                wc.args.toArray());
    }

    /** 构建创建时间范围条件（含端点当天），start/end 形如 yyyy-MM-dd */
    private static Range range(String alias, String start, String end) {
        Range r = new Range();
        if (start != null && !start.trim().isEmpty()) {
            r.sql += " AND " + alias + ".created_at >= ?";
            r.args.add(start.trim());
        }
        if (end != null && !end.trim().isEmpty()) {
            r.sql += " AND " + alias + ".created_at < DATE_ADD(?, INTERVAL 1 DAY)";
            r.args.add(end.trim());
        }
        return r;
    }

    private int cnt(String sql, List<Object> args) {
        Integer v = jdbcTemplate.queryForObject(sql, Integer.class, args.toArray());
        return v == null ? 0 : v;
    }

    private BigDecimal avg(String sql, List<Object> args) {
        BigDecimal v = jdbcTemplate.queryForObject(sql, BigDecimal.class, args.toArray());
        return v == null ? BigDecimal.ZERO : v;
    }

    private static BigDecimal scale(double v) {
        return BigDecimal.valueOf(Math.round(v * 100) / 100.0);
    }

    private static final class Range {
        private String sql = "";
        private final List<Object> args = new ArrayList<>();
    }
}