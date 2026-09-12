package com.aroura.sentinel.logistics.dao.tms;

import com.aroura.sentinel.logistics.model.tms.Carrier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 承运商主数据 DAO
 *
 * @author sentinel
 */
@Repository
public class CarrierDao {

    private final JdbcTemplate jdbcTemplate;

    public CarrierDao(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Long insert(Carrier c) {
        jdbcTemplate.update(
                "INSERT INTO carrier (carrier_code, carrier_name, type, country, api_endpoint, api_key, status) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?)",
                c.getCarrierCode(), c.getCarrierName(), c.getType(), c.getCountry(),
                c.getApiEndpoint(), c.getApiKey(), c.getStatus());
        Number key = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Number.class);
        return key == null ? null : key.longValue();
    }

    public void update(Carrier c) {
        jdbcTemplate.update(
                "UPDATE carrier SET carrier_name=?, type=?, country=?, api_endpoint=?, api_key=?, status=? WHERE id=? AND is_deleted=0",
                c.getCarrierName(), c.getType(), c.getCountry(), c.getApiEndpoint(), c.getApiKey(), c.getStatus(), c.getId());
    }

    public void delete(Long id) {
        jdbcTemplate.update("UPDATE carrier SET is_deleted = 1 WHERE id = ?", id);
    }

    public Map<String, Object> findById(Long id) {
        List<Map<String, Object>> list = jdbcTemplate.queryForList(
                "SELECT * FROM carrier WHERE id = ? AND is_deleted = 0", id);
        return list.isEmpty() ? null : list.get(0);
    }

    public Map<String, Object> findPage(String keyword, int page, int perPage) {
        StringBuilder where = new StringBuilder(" WHERE is_deleted = 0");
        List<Object> args = new java.util.ArrayList<>();
        if (keyword != null && !keyword.trim().isEmpty()) {
            where.append(" AND (carrier_code LIKE ? OR carrier_name LIKE ?)");
            args.add("%" + keyword.trim() + "%");
            args.add("%" + keyword.trim() + "%");
        }
        String whereSql = where.toString();
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM carrier" + whereSql, Integer.class, args.toArray());
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT * FROM carrier" + whereSql + " ORDER BY id DESC LIMIT ? OFFSET ?",
                java.util.stream.Stream.concat(args.stream(), java.util.stream.Stream.of(perPage, (page - 1) * perPage)).toArray());
        Map<String, Object> result = new HashMap<>(4);
        result.put("count", count == null ? 0 : count);
        result.put("rows", rows);
        return result;
    }

    public List<Map<String, Object>> listAll() {
        return jdbcTemplate.queryForList("SELECT id, carrier_code, carrier_name, type FROM carrier WHERE is_deleted = 0 AND status = 1 ORDER BY id ASC");
    }
}
