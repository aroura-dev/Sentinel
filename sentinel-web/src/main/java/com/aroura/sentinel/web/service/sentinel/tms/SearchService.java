package com.aroura.sentinel.web.service.sentinel.tms;

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

    /**
     * 全局搜索。
     *
     * @param merchantScope 为 null 表示不限制（平台角色）；非 null 时订单/运单限定该商家，
     *                      且<b>不返回商家名录</b> —— 商家无需搜索其他商家
     */
    public Map<String, Object> search(String keyword, Long merchantScope) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return empty();
        }
        String k = "%" + keyword.trim() + "%";
        StringBuilder orderSql = new StringBuilder(
                "SELECT id, order_no, merchant_name, destination_country, current_node, sla_status, freight_cost, waybill_no "
                        + "FROM logistics_order WHERE is_deleted = 0 "
                        + "AND (order_no LIKE ? OR waybill_no LIKE ? OR buyer_id LIKE ? OR buyer_phone LIKE ?)");
        List<Object> orderArgs = new java.util.ArrayList<>();
        java.util.Collections.addAll(orderArgs, k, k, k, k);
        if (merchantScope != null) {
            orderSql.append(" AND merchant_id = ?");
            orderArgs.add(merchantScope);
        }
        List<Map<String, Object>> orders = jdbcTemplate.queryForList(
                orderSql.append(" ORDER BY id DESC LIMIT 5").toString(), orderArgs.toArray());

        StringBuilder waybillSql = new StringBuilder(
                "SELECT waybill_no, tracking_no, order_no, carrier_code, status, freight_cost "
                        + "FROM waybill WHERE is_deleted = 0 "
                        + "AND (waybill_no LIKE ? OR tracking_no LIKE ? OR order_no LIKE ?)");
        List<Object> waybillArgs = new java.util.ArrayList<>();
        java.util.Collections.addAll(waybillArgs, k, k, k);
        if (merchantScope != null) {
            waybillSql.append(" AND merchant_id = ?");
            waybillArgs.add(merchantScope);
        }
        List<Map<String, Object>> waybills = jdbcTemplate.queryForList(
                waybillSql.append(" ORDER BY id DESC LIMIT 5").toString(), waybillArgs.toArray());

        List<Map<String, Object>> merchants;
        if (merchantScope != null) {
            merchants = java.util.Collections.emptyList();
        } else {
            merchants = jdbcTemplate.queryForList(
                    "SELECT id, merchant_code, merchant_name, status "
                            + "FROM merchant WHERE is_deleted = 0 "
                            + "AND (merchant_name LIKE ? OR merchant_code LIKE ?) LIMIT 5",
                    k, k);
        }

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
