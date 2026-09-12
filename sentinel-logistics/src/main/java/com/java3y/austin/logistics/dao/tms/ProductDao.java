package com.java3y.austin.logistics.dao.tms;

import com.java3y.austin.logistics.model.tms.Product;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 商品 SKU DAO
 *
 * @author sentinel
 */
@Repository
public class ProductDao {

    private final JdbcTemplate jdbcTemplate;

    public ProductDao(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Long insert(Product p) {
        jdbcTemplate.update(
                "INSERT INTO product (merchant_id, sku, name, hs_code, declared_value, currency, weight_kg, volume_l, origin_country, status) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                p.getMerchantId(), p.getSku(), p.getName(), p.getHsCode(), p.getDeclaredValue(), p.getCurrency(),
                p.getWeightKg(), p.getVolumeL(), p.getOriginCountry(), p.getStatus());
        Number key = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Number.class);
        return key == null ? null : key.longValue();
    }

    public void update(Product p) {
        jdbcTemplate.update(
                "UPDATE product SET merchant_id=?, sku=?, name=?, hs_code=?, declared_value=?, currency=?, weight_kg=?, volume_l=?, origin_country=?, status=? "
                        + "WHERE id=? AND is_deleted=0",
                p.getMerchantId(), p.getSku(), p.getName(), p.getHsCode(), p.getDeclaredValue(), p.getCurrency(),
                p.getWeightKg(), p.getVolumeL(), p.getOriginCountry(), p.getStatus(), p.getId());
    }

    public void delete(Long id) {
        jdbcTemplate.update("UPDATE product SET is_deleted = 1 WHERE id = ?", id);
    }

    public Map<String, Object> findById(Long id) {
        List<Map<String, Object>> list = jdbcTemplate.queryForList(
                "SELECT * FROM product WHERE id = ? AND is_deleted = 0", id);
        return list.isEmpty() ? null : list.get(0);
    }

    public Map<String, Object> findByMerchantSku(Long merchantId, String sku) {
        List<Map<String, Object>> list = jdbcTemplate.queryForList(
                "SELECT * FROM product WHERE merchant_id = ? AND sku = ? AND is_deleted = 0 LIMIT 1", merchantId, sku);
        return list.isEmpty() ? null : list.get(0);
    }

    public Map<String, Object> findPage(Long merchantId, String keyword, int page, int perPage) {
        StringBuilder where = new StringBuilder(" WHERE is_deleted = 0");
        List<Object> args = new java.util.ArrayList<>();
        if (merchantId != null) {
            where.append(" AND merchant_id = ?");
            args.add(merchantId);
        }
        if (keyword != null && !keyword.trim().isEmpty()) {
            where.append(" AND (sku LIKE ? OR name LIKE ?)");
            args.add("%" + keyword.trim() + "%");
            args.add("%" + keyword.trim() + "%");
        }
        String whereSql = where.toString();
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM product" + whereSql, Integer.class, args.toArray());
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT * FROM product" + whereSql + " ORDER BY id DESC LIMIT ? OFFSET ?",
                java.util.stream.Stream.concat(args.stream(), java.util.stream.Stream.of(perPage, (page - 1) * perPage)).toArray());
        Map<String, Object> result = new HashMap<>(4);
        result.put("count", count == null ? 0 : count);
        result.put("rows", rows);
        return result;
    }

    public List<Map<String, Object>> listByMerchant(Long merchantId) {
        return jdbcTemplate.queryForList(
                "SELECT id, merchant_id, sku, name, hs_code, declared_value, currency, weight_kg, volume_l FROM product "
                        + "WHERE merchant_id = ? AND is_deleted = 0 AND status = 1 ORDER BY id ASC", merchantId);
    }
}
