package com.java3y.austin.web.service.sentinel.tms;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * TMS 看板服务：平台/卖家视角统计
 *
 * @author sentinel
 */
@Service
public class TmsDashboardService {

    private final JdbcTemplate jdbcTemplate;

    public TmsDashboardService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Map<String, Object> overview() {
        Map<String, Object> result = new HashMap<>(8);
        result.put("totalOrders", cnt("SELECT COUNT(*) FROM logistics_order WHERE is_deleted = 0"));
        result.put("inTransit", cnt("SELECT COUNT(*) FROM logistics_order WHERE is_deleted = 0 AND current_node NOT IN ('DELIVERED','LOST','RETURNED')"));
        result.put("delivered", cnt("SELECT COUNT(*) FROM logistics_order WHERE is_deleted = 0 AND current_node = 'DELIVERED'"));
        result.put("slaRisk", cnt("SELECT COUNT(*) FROM logistics_order WHERE is_deleted = 0 AND sla_status IN ('RISK','BREACHED')"));
        result.put("anomaly", cnt("SELECT COUNT(*) FROM logistics_order WHERE is_deleted = 0 AND current_node IN ('CUSTOMS_DELAY','DELIVERY_FAILED','LOST','RETURNED')"));
        result.put("totalFreight", jdbcTemplate.queryForObject(
                "SELECT COALESCE(SUM(freight_cost), 0) FROM logistics_order WHERE is_deleted = 0 AND freight_cost > 0", java.math.BigDecimal.class));
        result.put("openWorkorders", cnt("SELECT COUNT(*) FROM workorder WHERE is_deleted = 0 AND status IN ('OPEN','PROCESSING')"));
        result.put("bills", jdbcTemplate.queryForList(
                "SELECT status, COUNT(*) AS cnt, COALESCE(SUM(total_amount),0) AS amount FROM bill WHERE is_deleted = 0 GROUP BY status"));
        return result;
    }

    public Map<String, Object> seller(Long merchantId) {
        Map<String, Object> result = new HashMap<>(8);
        // merchantId 为 null 时聚合全部商家（ADMIN/OPERATOR 视角）
        String scope = merchantId == null ? "" : " AND merchant_id = " + merchantId;
        result.put("totalOrders", cnt("SELECT COUNT(*) FROM logistics_order WHERE is_deleted = 0" + scope));
        result.put("inTransit", cnt("SELECT COUNT(*) FROM logistics_order WHERE is_deleted = 0" + scope + " AND current_node NOT IN ('DELIVERED','LOST','RETURNED')"));
        result.put("delivered", cnt("SELECT COUNT(*) FROM logistics_order WHERE is_deleted = 0" + scope + " AND current_node = 'DELIVERED'"));
        result.put("slaRisk", cnt("SELECT COUNT(*) FROM logistics_order WHERE is_deleted = 0" + scope + " AND sla_status IN ('RISK','BREACHED')"));
        result.put("totalFreight", jdbcTemplate.queryForObject(
                "SELECT COALESCE(SUM(freight_cost), 0) FROM logistics_order WHERE is_deleted = 0" + scope + " AND freight_cost > 0", java.math.BigDecimal.class));
        result.put("bills", jdbcTemplate.queryForList(
                "SELECT bill.status, COUNT(*) AS cnt, COALESCE(SUM(bill_item.freight_cost),0) AS amount "
                        + "FROM bill_item JOIN bill ON bill_item.bill_id = bill.id "
                        + "WHERE bill_item.merchant_id = " + merchantId + " AND bill_item.is_deleted = 0 AND bill.is_deleted = 0 "
                        + "GROUP BY bill.status"));
        return result;
    }

    public List<Map<String, Object>> orderTrend() {
        return jdbcTemplate.queryForList(
                "SELECT DATE(created_at) AS date, COUNT(*) AS cnt FROM logistics_order "
                        + "WHERE is_deleted = 0 AND created_at >= DATE_SUB(CURDATE(), INTERVAL 6 DAY) "
                        + "GROUP BY DATE(created_at) ORDER BY date");
    }

    public List<Map<String, Object>> carrierVolume() {
        return jdbcTemplate.queryForList(
                "SELECT c.carrier_name, c.carrier_code, COUNT(o.id) AS cnt "
                        + "FROM logistics_order o JOIN carrier c ON o.carrier_id = c.id "
                        + "WHERE o.is_deleted = 0 AND o.carrier_id IS NOT NULL "
                        + "GROUP BY c.carrier_name, c.carrier_code ORDER BY cnt DESC");
    }

    public List<Map<String, Object>> channelMix() {
        return jdbcTemplate.queryForList(
                "SELECT c.channel_code, c.channel_name, COUNT(o.id) AS cnt "
                        + "FROM logistics_order o JOIN carrier_channel c ON o.channel_id = c.id "
                        + "WHERE o.is_deleted = 0 AND o.channel_id IS NOT NULL GROUP BY c.channel_code, c.channel_name ORDER BY cnt DESC");
    }

    public List<Map<String, Object>> anomalyRate() {
        return jdbcTemplate.queryForList(
                "SELECT current_node AS node, COUNT(*) AS cnt FROM logistics_order "
                        + "WHERE is_deleted = 0 AND current_node IN ('CUSTOMS_DELAY','DELIVERY_FAILED','LOST','RETURNED') "
                        + "GROUP BY current_node ORDER BY cnt DESC");
    }

    public List<Map<String, Object>> settlement() {
        return jdbcTemplate.queryForList(
                "SELECT ca.carrier_name, b.status, COUNT(*) AS cnt, COALESCE(SUM(b.total_amount),0) AS amount "
                        + "FROM bill b JOIN carrier ca ON b.carrier_id = ca.id "
                        + "WHERE b.is_deleted = 0 GROUP BY ca.carrier_name, b.status ORDER BY ca.carrier_name");
    }

    private Integer cnt(String sql) {
        Integer v = jdbcTemplate.queryForObject(sql, Integer.class);
        return v == null ? 0 : v;
    }
}
