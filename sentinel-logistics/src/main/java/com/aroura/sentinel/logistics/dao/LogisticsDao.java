package com.aroura.sentinel.logistics.dao;

import com.aroura.sentinel.logistics.model.LogisticsOrder;
import com.aroura.sentinel.logistics.model.LogisticsTrack;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 物流订单/轨迹 DAO（JdbcTemplate，落库 logistics_order / logistics_track）
 * <p>
 * 修复：原 LogisticsController 仅用内存 Map 存储订单，重启即丢。
 *
 * @author sentinel
 */
@Repository
public class LogisticsDao {

    private final JdbcTemplate jdbcTemplate;

    public LogisticsDao(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void saveOrder(LogisticsOrder order) {
        jdbcTemplate.update(
                "INSERT INTO logistics_order (order_no, buyer_id, buyer_phone, buyer_language, merchant_name, destination_country, current_node) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?)",
                order.getOrderNo(), order.getBuyerId(), order.getBuyerPhone(), order.getBuyerLanguage(),
                order.getMerchantName(), order.getDestinationCountry(), order.getCurrentNode());
    }

    public void updateOrderCurrentNode(String orderNo, String node) {
        jdbcTemplate.update("UPDATE logistics_order SET current_node = ? WHERE order_no = ? AND is_deleted = 0", node, orderNo);
    }

    /**
     * 人工编辑订单信息（买家/地址/语言/业务备注）
     */
    public void updateOrderInfo(String orderNo, String buyerPhone, String buyerAddress,
                                String buyerCity, String buyerPostal, String buyerLanguage, String businessNotes) {
        jdbcTemplate.update(
                "UPDATE logistics_order SET buyer_phone=?, buyer_address=?, buyer_city=?, buyer_postal=?, buyer_language=?, business_notes=? "
                        + "WHERE order_no=? AND is_deleted=0",
                buyerPhone, buyerAddress, buyerCity, buyerPostal, buyerLanguage, businessNotes, orderNo);
    }

    /**
     * 保存 TMS 履约订单（含渠道/商品/运费快照等完整字段）
     */
    public void saveTmsOrder(LogisticsOrder order) {
        jdbcTemplate.update(
                "INSERT INTO logistics_order (order_no, buyer_id, buyer_name, buyer_phone, buyer_language, merchant_id, merchant_name, destination_country, "
                        + "current_node, channel_id, carrier_id, warehouse_id, items_json, declared_value, declared_currency, freight_cost, freight_currency, "
                        + "promise_eta, sla_status, waybill_no, buyer_address, buyer_city, buyer_postal, business_notes) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                order.getOrderNo(), order.getBuyerId(), order.getBuyerName(), order.getBuyerPhone(), order.getBuyerLanguage(),
                order.getMerchantId(), order.getMerchantName(), order.getDestinationCountry(),
                order.getCurrentNode(), order.getChannelId(), order.getCarrierId(), order.getWarehouseId(),
                order.getItemsJson(), order.getDeclaredValue(), order.getDeclaredCurrency(),
                order.getFreightCost(), order.getFreightCurrency(),
                order.getPromiseEta(), order.getSlaStatus(), order.getWaybillNo(),
                order.getBuyerAddress(), order.getBuyerCity(), order.getBuyerPostal(), order.getBusinessNotes());
    }

    /**
     * 出库生成运单：回写运单号/承诺ETA，节点推进到 WAREHOUSE_OUT，SLA 置 NORMAL
     */
    public void updateOrderWaybill(String orderNo, String waybillNo, java.util.Date promiseEta) {
        jdbcTemplate.update(
                "UPDATE logistics_order SET waybill_no=?, promise_eta=?, sla_status='NORMAL', current_node='WAREHOUSE_OUT' "
                        + "WHERE order_no=? AND is_deleted=0", waybillNo, promiseEta, orderNo);
    }

    public void updateSlaStatus(String orderNo, String slaStatus) {
        jdbcTemplate.update("UPDATE logistics_order SET sla_status=? WHERE order_no=? AND is_deleted=0", slaStatus, orderNo);
    }

    /**
     * TMS 订单分页（支持商家/渠道/SLA/节点/订单号关键字过滤）
     */
    public Map<String, Object> findPageTms(Long merchantId, Long channelId, String slaStatus, String node,
                                           String startDate, String endDate, String keyword, int page, int perPage) {
        StringBuilder where = new StringBuilder(" WHERE is_deleted = 0");
        java.util.List<Object> args = new java.util.ArrayList<>();
        if (merchantId != null) {
            where.append(" AND merchant_id = ?");
            args.add(merchantId);
        }
        if (channelId != null) {
            where.append(" AND channel_id = ?");
            args.add(channelId);
        }
        if (slaStatus != null && !slaStatus.trim().isEmpty()) {
            where.append(" AND sla_status = ?");
            args.add(slaStatus.trim());
        }
        if (node != null && !node.trim().isEmpty()) {
            // ANOMALY = 全部异常节点（中转延误/派送失败/丢件/退回）
            if ("ANOMALY".equalsIgnoreCase(node.trim())) {
                where.append(" AND current_node IN ('CUSTOMS_DELAY','DELIVERY_FAILED','LOST','RETURNED')");
            } else {
                where.append(" AND current_node = ?");
                args.add(node.trim());
            }
        }
        if (keyword != null && !keyword.trim().isEmpty()) {
            where.append(" AND order_no LIKE ?");
            args.add("%" + keyword.trim() + "%");
        }
        if (startDate != null && !startDate.trim().isEmpty()) {
            where.append(" AND created_at >= ?");
            args.add(startDate.trim() + " 00:00:00");
        }
        if (endDate != null && !endDate.trim().isEmpty()) {
            where.append(" AND created_at <= ?");
            args.add(endDate.trim() + " 23:59:59");
        }
        String whereSql = where.toString();
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM logistics_order" + whereSql, Integer.class, args.toArray());
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT * FROM logistics_order" + whereSql + " ORDER BY id DESC LIMIT ? OFFSET ?",
                java.util.stream.Stream.concat(args.stream(), java.util.stream.Stream.of(perPage, (page - 1) * perPage)).toArray());
        Map<String, Object> result = new HashMap<>(4);
        result.put("count", count == null ? 0 : count);
        result.put("rows", rows);
        return result;
    }

    public void saveTrack(LogisticsTrack track) {
        jdbcTemplate.update(
                "INSERT INTO logistics_track (order_no, node, raw_status, raw_desc, location, carrier_code, track_time) "
                        + "VALUES (?, ?, ?, ?, ?, ?, FROM_UNIXTIME(?/1000))",
                track.getOrderNo(), track.getNode(), track.getRawStatus(), track.getRawDesc(), track.getLocation(),
                track.getCarrierCode(),
                track.getTrackTime() == null ? System.currentTimeMillis() : track.getTrackTime());
    }

    public Map<String, Object> findOrderByNo(String orderNo) {
        List<Map<String, Object>> list = jdbcTemplate.queryForList(
                "SELECT * FROM logistics_order WHERE order_no = ? AND is_deleted = 0", orderNo);
        return list.isEmpty() ? null : list.get(0);
    }

    public Map<String, Object> listOrders(String orderNo, String status, int page, int perPage) {
        StringBuilder where = new StringBuilder(" WHERE is_deleted = 0");
        java.util.List<Object> args = new java.util.ArrayList<>();
        if (orderNo != null && !orderNo.trim().isEmpty()) {
            where.append(" AND order_no LIKE ?");
            args.add("%" + orderNo.trim() + "%");
        }
        if (status != null && !status.trim().isEmpty()) {
            where.append(" AND current_node = ?");
            args.add(status.trim());
        }
        String whereSql = where.toString();
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM logistics_order" + whereSql, Integer.class, args.toArray());
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT * FROM logistics_order" + whereSql + " ORDER BY id DESC LIMIT ? OFFSET ?",
                java.util.stream.Stream.concat(args.stream(), java.util.stream.Stream.of(perPage, (page - 1) * perPage)).toArray());
        Map<String, Object> result = new HashMap<>(4);
        result.put("count", count == null ? 0 : count);
        result.put("rows", rows);
        return result;
    }

    public List<Map<String, Object>> listTracks(String orderNo) {
        return jdbcTemplate.queryForList(
                "SELECT * FROM logistics_track WHERE order_no = ? AND is_deleted = 0 ORDER BY track_time ASC, id ASC", orderNo);
    }

    /**
     * 查询某节点滞留超过 N 小时的订单（供异常扫描）
     */
    public List<Map<String, Object>> listStuckOrders(String node, int hours) {
        // hours 为常量阈值（非用户输入），内联避免 MySQL INTERVAL 占位符兼容问题
        return jdbcTemplate.queryForList(
                "SELECT * FROM logistics_order WHERE current_node = ? AND is_deleted = 0 "
                        + "AND updated_at < DATE_SUB(NOW(), INTERVAL " + hours + " HOUR)", node);
    }

    public List<Map<String, Object>> listOrdersByNodes(List<String> nodes) {
        if (nodes == null || nodes.isEmpty()) {
            return java.util.Collections.emptyList();
        }
        String placeholders = String.join(",", java.util.Collections.nCopies(nodes.size(), "?"));
        return jdbcTemplate.queryForList(
                "SELECT * FROM logistics_order WHERE is_deleted = 0 AND current_node IN (" + placeholders + ") ORDER BY id ASC",
                nodes.toArray());
    }

    /* ---------- 订单审核 ---------- */

    public void updateReviewStatus(String orderNo, String status) {
        jdbcTemplate.update("UPDATE logistics_order SET review_status=? WHERE order_no=? AND is_deleted=0", status, orderNo);
    }

    public void updateReviewResult(String orderNo, String status, String reviewedBy, String rejectReason) {
        jdbcTemplate.update(
                "UPDATE logistics_order SET review_status=?, reviewed_by=?, reviewed_at=NOW(), reject_reason=? WHERE order_no=? AND is_deleted=0",
                status, reviewedBy, rejectReason, orderNo);
    }

    /**
     * 待审核/已驳回订单分页（审核完成后 APPROVED 不再出现）
     */
    public Map<String, Object> findReviewPage(String reviewStatus, String keyword, int page, int perPage) {
        StringBuilder where = new StringBuilder(" WHERE is_deleted = 0");
        List<Object> args = new java.util.ArrayList<>();
        if (reviewStatus != null && !reviewStatus.trim().isEmpty()) {
            where.append(" AND review_status = ?");
            args.add(reviewStatus.trim());
        } else {
            where.append(" AND review_status IN ('PENDING','REJECTED')");
        }
        if (keyword != null && !keyword.trim().isEmpty()) {
            where.append(" AND (order_no LIKE ? OR merchant_name LIKE ?)");
            args.add("%" + keyword.trim() + "%");
            args.add("%" + keyword.trim() + "%");
        }
        String whereSql = where.toString();
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM logistics_order" + whereSql, Integer.class, args.toArray());
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT * FROM logistics_order" + whereSql + " ORDER BY id DESC LIMIT ? OFFSET ?",
                java.util.stream.Stream.concat(args.stream(), java.util.stream.Stream.of(perPage, (page - 1) * perPage)).toArray());
        Map<String, Object> result = new HashMap<>(4);
        result.put("count", count == null ? 0 : count);
        result.put("rows", rows);
        return result;
    }
}