package com.aroura.sentinel.logistics.dao.tms;

import com.aroura.sentinel.logistics.model.tms.CarrierRate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 运费价卡 DAO
 *
 * @author sentinel
 */
@Repository
public class CarrierRateDao {

    private final JdbcTemplate jdbcTemplate;

    public CarrierRateDao(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Long insert(CarrierRate r) {
        jdbcTemplate.update(
                "INSERT INTO carrier_rate (channel_id, zone, min_weight_kg, max_weight_kg, mode, first_weight_kg, first_price, "
                        + "continued_weight_kg, continued_price, price, currency, effective_from, effective_to, status) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                r.getChannelId(), r.getZone(), r.getMinWeightKg(), r.getMaxWeightKg(), r.getMode(),
                r.getFirstWeightKg(), r.getFirstPrice(), r.getContinuedWeightKg(), r.getContinuedPrice(),
                r.getPrice(), r.getCurrency(), r.getEffectiveFrom(), r.getEffectiveTo(), r.getStatus());
        Number key = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Number.class);
        return key == null ? null : key.longValue();
    }

    public void update(CarrierRate r) {
        jdbcTemplate.update(
                "UPDATE carrier_rate SET channel_id=?, zone=?, min_weight_kg=?, max_weight_kg=?, mode=?, first_weight_kg=?, first_price=?, "
                        + "continued_weight_kg=?, continued_price=?, price=?, currency=?, effective_from=?, effective_to=?, status=? "
                        + "WHERE id=? AND is_deleted=0",
                r.getChannelId(), r.getZone(), r.getMinWeightKg(), r.getMaxWeightKg(), r.getMode(),
                r.getFirstWeightKg(), r.getFirstPrice(), r.getContinuedWeightKg(), r.getContinuedPrice(),
                r.getPrice(), r.getCurrency(), r.getEffectiveFrom(), r.getEffectiveTo(), r.getStatus(), r.getId());
    }

    public void delete(Long id) {
        jdbcTemplate.update("UPDATE carrier_rate SET is_deleted = 1 WHERE id = ?", id);
    }

    public Map<String, Object> findById(Long id) {
        List<Map<String, Object>> list = jdbcTemplate.queryForList(
                "SELECT * FROM carrier_rate WHERE id = ? AND is_deleted = 0", id);
        return list.isEmpty() ? null : list.get(0);
    }

    public Map<String, Object> findPage(Long channelId, String zone, int page, int perPage) {
        StringBuilder where = new StringBuilder(" WHERE is_deleted = 0");
        List<Object> args = new java.util.ArrayList<>();
        if (channelId != null) {
            where.append(" AND channel_id = ?");
            args.add(channelId);
        }
        if (zone != null && !zone.trim().isEmpty()) {
            where.append(" AND zone = ?");
            args.add(zone.trim());
        }
        String whereSql = where.toString();
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM carrier_rate" + whereSql, Integer.class, args.toArray());
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT * FROM carrier_rate" + whereSql + " ORDER BY id DESC LIMIT ? OFFSET ?",
                java.util.stream.Stream.concat(args.stream(), java.util.stream.Stream.of(perPage, (page - 1) * perPage)).toArray());
        Map<String, Object> result = new HashMap<>(4);
        result.put("count", count == null ? 0 : count);
        result.put("rows", rows);
        return result;
    }

    /**
     * 命中价卡查询：区域优先具体 zone，回退 DEFAULT；生效期内；重量落在重量段
     */
    public Map<String, Object> selectRate(Long channelId, String zone, BigDecimal weightKg, java.util.Date date) {
        java.sql.Date d = new java.sql.Date(date.getTime());
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT * FROM carrier_rate WHERE channel_id = ? AND zone IN (?, 'DEFAULT') AND status = 1 AND is_deleted = 0 "
                        + "AND effective_from <= ? AND (effective_to IS NULL OR effective_to >= ?) "
                        + "AND ? >= min_weight_kg AND (max_weight_kg IS NULL OR ? <= max_weight_kg) "
                        + "ORDER BY (zone = 'DEFAULT') ASC, effective_from DESC LIMIT 1",
                channelId, zone, d, d, weightKg, weightKg);
        return rows.isEmpty() ? null : rows.get(0);
    }

    public List<Map<String, Object>> listByChannel(Long channelId) {
        return jdbcTemplate.queryForList(
                "SELECT * FROM carrier_rate WHERE channel_id = ? AND is_deleted = 0 ORDER BY zone, min_weight_kg ASC", channelId);
    }
}
