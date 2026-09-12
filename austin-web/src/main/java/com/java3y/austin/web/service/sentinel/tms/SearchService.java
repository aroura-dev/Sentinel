package com.java3y.austin.web.service.sentinel.tms;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 全局搜索：一个关键词跨 订单 / 运单 / 商家 匹配，供顶部搜索框使用
 *
 * @author sentinel
 */
@Service
public class SearchService {

    private final JdbcTemplate jdbcTemplate;

    public SearchService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Map<String, Object> search(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return empty();
        }
        String k = "%" + keyword.trim() + "%";
        List<Map<String, Object>> orders = jdbcTemplate.queryForList(
                "SELECT id, order_no, merchant_name, destination_country, current_node, sla_status, freight_cost, waybill_no "
                        + "FROM logistics_order WHERE is_deleted = 0 "
                        + "AND (order_no LIKE ? OR waybill_no LIKE ? OR buyer_id LIKE ? OR buyer_phone LIKE ?) "
                        + "ORDER BY id DESC LIMIT 5",
                k, k, k, k);
        List<Map<String, Object>> waybills = jdbcTemplate.queryForList(
                "SELECT waybill_no, tracking_no, order_no, carrier_code, status, freight_cost "
                        + "FROM waybill WHERE is_deleted = 0 "
                        + "AND (waybill_no LIKE ? OR tracking_no LIKE ? OR order_no LIKE ?) "
                        + "ORDER BY id DESC LIMIT 5",
                k, k, k);
        List<Map<String, Object>> merchants = jdbcTemplate.queryForList(
                "SELECT id, merchant_code, merchant_name, status "
                        + "FROM merchant WHERE is_deleted = 0 "
                        + "AND (merchant_name LIKE ? OR merchant_code LIKE ?) LIMIT 5",
                k, k);

        Map<String, Object> result = new HashMap<>(4);
        result.put("orders", orders);
        result.put("waybills", waybills);
        result.put("merchants", merchants);
        return result;
    }

    private static Map<String, Object> empty() {
        Map<String, Object> result = new HashMap<>(4);
        result.put("orders", java.util.Collections.emptyList());
        result.put("waybills", java.util.Collections.emptyList());
        result.put("merchants", java.util.Collections.emptyList());
        return result;
    }
}
