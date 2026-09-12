package com.java3y.austin.logistics.dao.tms;

import com.java3y.austin.logistics.model.tms.Waybill;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 运单 DAO
 *
 * @author sentinel
 */
@Repository
public class WaybillDao {

    private final JdbcTemplate jdbcTemplate;

    public WaybillDao(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Long insert(Waybill w) {
        jdbcTemplate.update(
                "INSERT INTO waybill (waybill_no, order_no, merchant_id, channel_id, carrier_id, tracking_no, carrier_code, "
                        + "weight_kg, volume_l, billable_weight_kg, declared_value, declared_currency, freight_cost, freight_currency, "
                        + "zone, promise_eta, status, billed, items_json) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                w.getWaybillNo(), w.getOrderNo(), w.getMerchantId(), w.getChannelId(), w.getCarrierId(), w.getTrackingNo(), w.getCarrierCode(),
                w.getWeightKg(), w.getVolumeL(), w.getBillableWeightKg(), w.getDeclaredValue(), w.getDeclaredCurrency(),
                w.getFreightCost(), w.getFreightCurrency(), w.getZone(), w.getPromiseEta(), w.getStatus(), w.getBilled(),
                w.getItemsJson());
        Number key = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Number.class);
        return key == null ? null : key.longValue();
    }

    /**
     * 分批出库场景：订单全部妥投时，其下所有运单一并标记 DELIVERED
     */
    public void markDeliveredByOrderNo(String orderNo, Date actualTime) {
        jdbcTemplate.update("UPDATE waybill SET status = 'DELIVERED', actual_delivered_at = ? "
                        + "WHERE order_no = ? AND is_deleted = 0",
                actualTime, orderNo);
    }

    public Map<String, Object> findById(Long id) {
        List<Map<String, Object>> list = jdbcTemplate.queryForList(
                "SELECT * FROM waybill WHERE id = ? AND is_deleted = 0", id);
        return list.isEmpty() ? null : list.get(0);
    }

    public Map<String, Object> findByWaybillNo(String waybillNo) {
        List<Map<String, Object>> list = jdbcTemplate.queryForList(
                "SELECT * FROM waybill WHERE waybill_no = ? AND is_deleted = 0", waybillNo);
        return list.isEmpty() ? null : list.get(0);
    }

    public Map<String, Object> findByOrderNo(String orderNo) {
        List<Map<String, Object>> list = jdbcTemplate.queryForList(
                "SELECT * FROM waybill WHERE order_no = ? AND is_deleted = 0 LIMIT 1", orderNo);
        return list.isEmpty() ? null : list.get(0);
    }

    public void markDelivered(String waybillNo, Date actualTime) {
        jdbcTemplate.update("UPDATE waybill SET status = 'DELIVERED', actual_delivered_at = ? WHERE waybill_no = ? AND is_deleted = 0",
                actualTime, waybillNo);
    }

    public void markBilled(Long id) {
        jdbcTemplate.update("UPDATE waybill SET billed = 1 WHERE id = ? AND is_deleted = 0", id);
    }

    /**
     * 某承运商某账期内出库、未入账的运单（对账入账按出库时间）
     */
    public List<Map<String, Object>> listUnbilled(Long carrierId, Date periodStart, Date periodEnd) {
        return jdbcTemplate.queryForList(
                "SELECT * FROM waybill WHERE carrier_id = ? AND is_deleted = 0 AND billed = 0 "
                        + "AND created_at >= ? AND created_at < DATE_ADD(?, INTERVAL 1 DAY) ORDER BY id ASC",
                carrierId, periodStart, periodEnd);
    }

    /**
     * 在途且有运单/渠道的订单（SLA 扫描用），返回 promise_eta/transit_days_max/出库时间
     */
    public List<Map<String, Object>> listActiveOrdersWithSla() {
        return jdbcTemplate.queryForList(
                "SELECT o.order_no, o.current_node, o.promise_eta, c.transit_days_max, w.created_at AS outbound_at "
                        + "FROM logistics_order o JOIN waybill w ON o.waybill_no = w.waybill_no "
                        + "JOIN carrier_channel c ON o.channel_id = c.id "
                        + "WHERE o.is_deleted = 0 AND o.waybill_no IS NOT NULL "
                        + "AND o.current_node NOT IN ('DELIVERED','LOST','RETURNED')");
    }

    public Map<String, Object> findPage(String orderNo, String waybillNo, String trackingNo, Long channelId, Long carrierId, int page, int perPage) {
        StringBuilder where = new StringBuilder(" WHERE is_deleted = 0");
        List<Object> args = new java.util.ArrayList<>();
        if (orderNo != null && !orderNo.trim().isEmpty()) {
            where.append(" AND order_no LIKE ?");
            args.add("%" + orderNo.trim() + "%");
        }
        if (waybillNo != null && !waybillNo.trim().isEmpty()) {
            where.append(" AND waybill_no LIKE ?");
            args.add("%" + waybillNo.trim() + "%");
        }
        if (trackingNo != null && !trackingNo.trim().isEmpty()) {
            where.append(" AND tracking_no LIKE ?");
            args.add("%" + trackingNo.trim() + "%");
        }
        if (channelId != null) {
            where.append(" AND channel_id = ?");
            args.add(channelId);
        }
        if (carrierId != null) {
            where.append(" AND carrier_id = ?");
            args.add(carrierId);
        }
        String whereSql = where.toString();
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM waybill" + whereSql, Integer.class, args.toArray());
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT * FROM waybill" + whereSql + " ORDER BY id DESC LIMIT ? OFFSET ?",
                java.util.stream.Stream.concat(args.stream(), java.util.stream.Stream.of(perPage, (page - 1) * perPage)).toArray());
        Map<String, Object> result = new HashMap<>(4);
        result.put("count", count == null ? 0 : count);
        result.put("rows", rows);
        return result;
    }
}
