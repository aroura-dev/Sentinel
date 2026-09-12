package com.java3y.austin.logistics.dao.tms;

import com.java3y.austin.logistics.model.tms.CarrierChannel;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 物流渠道 DAO
 *
 * @author sentinel
 */
@Repository
public class CarrierChannelDao {

    private final JdbcTemplate jdbcTemplate;

    public CarrierChannelDao(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Long insert(CarrierChannel c) {
        jdbcTemplate.update(
                "INSERT INTO carrier_channel (carrier_id, channel_code, channel_name, type, dest_country, transit_days_min, transit_days_max, "
                        + "tracking_prefix, min_billable_weight_kg, vol_divisor, status, remark) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                c.getCarrierId(), c.getChannelCode(), c.getChannelName(), c.getType(), c.getDestCountry(),
                c.getTransitDaysMin(), c.getTransitDaysMax(), c.getTrackingPrefix(), c.getMinBillableWeightKg(),
                c.getVolDivisor(), c.getStatus(), c.getRemark());
        Number key = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Number.class);
        return key == null ? null : key.longValue();
    }

    public void update(CarrierChannel c) {
        jdbcTemplate.update(
                "UPDATE carrier_channel SET carrier_id=?, channel_code=?, channel_name=?, type=?, dest_country=?, transit_days_min=?, transit_days_max=?, "
                        + "tracking_prefix=?, min_billable_weight_kg=?, vol_divisor=?, status=?, remark=? WHERE id=? AND is_deleted=0",
                c.getCarrierId(), c.getChannelCode(), c.getChannelName(), c.getType(), c.getDestCountry(),
                c.getTransitDaysMin(), c.getTransitDaysMax(), c.getTrackingPrefix(), c.getMinBillableWeightKg(),
                c.getVolDivisor(), c.getStatus(), c.getRemark(), c.getId());
    }

    public void delete(Long id) {
        jdbcTemplate.update("UPDATE carrier_channel SET is_deleted = 1 WHERE id = ?", id);
    }

    public Map<String, Object> findById(Long id) {
        List<Map<String, Object>> list = jdbcTemplate.queryForList(
                "SELECT * FROM carrier_channel WHERE id = ? AND is_deleted = 0", id);
        return list.isEmpty() ? null : list.get(0);
    }

    public Map<String, Object> findByCode(String channelCode) {
        List<Map<String, Object>> list = jdbcTemplate.queryForList(
                "SELECT * FROM carrier_channel WHERE channel_code = ? AND is_deleted = 0 LIMIT 1", channelCode);
        return list.isEmpty() ? null : list.get(0);
    }

    public Map<String, Object> findPage(Long carrierId, String destCountry, String type, int page, int perPage) {
        StringBuilder where = new StringBuilder(" WHERE is_deleted = 0");
        List<Object> args = new java.util.ArrayList<>();
        if (carrierId != null) {
            where.append(" AND carrier_id = ?");
            args.add(carrierId);
        }
        if (destCountry != null && !destCountry.trim().isEmpty()) {
            where.append(" AND dest_country = ?");
            args.add(destCountry.trim());
        }
        if (type != null && !type.trim().isEmpty()) {
            where.append(" AND type = ?");
            args.add(type.trim());
        }
        String whereSql = where.toString();
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM carrier_channel" + whereSql, Integer.class, args.toArray());
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT * FROM carrier_channel" + whereSql + " ORDER BY id DESC LIMIT ? OFFSET ?",
                java.util.stream.Stream.concat(args.stream(), java.util.stream.Stream.of(perPage, (page - 1) * perPage)).toArray());
        Map<String, Object> result = new HashMap<>(4);
        result.put("count", count == null ? 0 : count);
        result.put("rows", rows);
        return result;
    }

    /**
     * 查询支持某目的地且启用的渠道（AI 渠道推荐 / 建单选渠道用）
     */
    public List<Map<String, Object>> listByDestCountry(String destCountry) {
        return jdbcTemplate.queryForList(
                "SELECT c.*, ca.carrier_code, ca.carrier_name FROM carrier_channel c "
                        + "LEFT JOIN carrier ca ON c.carrier_id = ca.id "
                        + "WHERE c.dest_country = ? AND c.status = 1 AND c.is_deleted = 0 "
                        + "AND (ca.is_deleted = 0 OR ca.is_deleted IS NULL) ORDER BY c.transit_days_max ASC",
                destCountry);
    }

    public List<Map<String, Object>> listAll() {
        return jdbcTemplate.queryForList(
                "SELECT c.*, ca.carrier_name FROM carrier_channel c "
                        + "LEFT JOIN carrier ca ON c.carrier_id = ca.id "
                        + "WHERE c.is_deleted = 0 AND c.status = 1 ORDER BY c.id ASC");
    }
}
