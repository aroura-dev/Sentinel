package com.java3y.austin.logistics.dao.tms;

import com.java3y.austin.logistics.model.tms.Warehouse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 仓库主数据 DAO
 *
 * @author sentinel
 */
@Repository
public class WarehouseDao {

    private final JdbcTemplate jdbcTemplate;

    public WarehouseDao(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Long insert(Warehouse w) {
        jdbcTemplate.update(
                "INSERT INTO warehouse (warehouse_code, warehouse_name, country, city, address, status) VALUES (?, ?, ?, ?, ?, ?)",
                w.getWarehouseCode(), w.getWarehouseName(), w.getCountry(), w.getCity(), w.getAddress(), w.getStatus());
        Number key = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Number.class);
        return key == null ? null : key.longValue();
    }

    public void update(Warehouse w) {
        jdbcTemplate.update(
                "UPDATE warehouse SET warehouse_name=?, country=?, city=?, address=?, status=? WHERE id=? AND is_deleted=0",
                w.getWarehouseName(), w.getCountry(), w.getCity(), w.getAddress(), w.getStatus(), w.getId());
    }

    public void delete(Long id) {
        jdbcTemplate.update("UPDATE warehouse SET is_deleted = 1 WHERE id = ?", id);
    }

    public Map<String, Object> findById(Long id) {
        List<Map<String, Object>> list = jdbcTemplate.queryForList(
                "SELECT * FROM warehouse WHERE id = ? AND is_deleted = 0", id);
        return list.isEmpty() ? null : list.get(0);
    }

    public Map<String, Object> findPage(String keyword, int page, int perPage) {
        StringBuilder where = new StringBuilder(" WHERE is_deleted = 0");
        List<Object> args = new java.util.ArrayList<>();
        if (keyword != null && !keyword.trim().isEmpty()) {
            where.append(" AND (warehouse_code LIKE ? OR warehouse_name LIKE ?)");
            args.add("%" + keyword.trim() + "%");
            args.add("%" + keyword.trim() + "%");
        }
        String whereSql = where.toString();
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM warehouse" + whereSql, Integer.class, args.toArray());
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT * FROM warehouse" + whereSql + " ORDER BY id DESC LIMIT ? OFFSET ?",
                java.util.stream.Stream.concat(args.stream(), java.util.stream.Stream.of(perPage, (page - 1) * perPage)).toArray());
        Map<String, Object> result = new HashMap<>(4);
        result.put("count", count == null ? 0 : count);
        result.put("rows", rows);
        return result;
    }

    public List<Map<String, Object>> listAll() {
        return jdbcTemplate.queryForList("SELECT id, warehouse_code, warehouse_name, country, city FROM warehouse WHERE is_deleted = 0 AND status = 1 ORDER BY id ASC");
    }
}
