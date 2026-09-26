package com.aroura.sentinel.logistics.dao.tms;

import com.aroura.sentinel.logistics.model.tms.Merchant;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 商家主数据 DAO
 *
 * @author sentinel
 */
@Repository
public class MerchantDao {

    private final JdbcTemplate jdbcTemplate;

    public MerchantDao(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Long insert(Merchant m) {
        jdbcTemplate.update(
                "INSERT INTO merchant (merchant_code, merchant_name, user_id, contact_name, contact_phone, contact_email, country, status) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                m.getMerchantCode(), m.getMerchantName(), m.getUserId(), m.getContactName(),
                m.getContactPhone(), m.getContactEmail(), m.getCountry(), m.getStatus());
        Number key = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Number.class);
        return key == null ? null : key.longValue();
    }

    public void update(Merchant m) {
        jdbcTemplate.update(
                "UPDATE merchant SET merchant_name=?, user_id=?, contact_name=?, contact_phone=?, contact_email=?, country=?, status=? WHERE id=? AND is_deleted=0",
                m.getMerchantName(), m.getUserId(), m.getContactName(), m.getContactPhone(),
                m.getContactEmail(), m.getCountry(), m.getStatus(), m.getId());
    }

    public void delete(Long id) {
        jdbcTemplate.update("UPDATE merchant SET is_deleted = 1 WHERE id = ?", id);
    }

    public Map<String, Object> findById(Long id) {
        List<Map<String, Object>> list = jdbcTemplate.queryForList(
                "SELECT * FROM merchant WHERE id = ? AND is_deleted = 0", id);
        return list.isEmpty() ? null : list.get(0);
    }

    public Map<String, Object> findByUserId(Long userId) {
        List<Map<String, Object>> list = jdbcTemplate.queryForList(
                "SELECT * FROM merchant WHERE user_id = ? AND is_deleted = 0 LIMIT 1", userId);
        return list.isEmpty() ? null : list.get(0);
    }

    public List<Map<String, Object>> findByUsername(String username) {
        return jdbcTemplate.queryForList(
                "SELECT m.* FROM merchant m JOIN sentinel_user u ON m.user_id = u.id "
                        + "WHERE u.username = ? AND m.is_deleted = 0 AND u.is_deleted = 0 LIMIT 1", username);
    }

    public Map<String, Object> findPage(String keyword, int page, int perPage) {
        return findPage(keyword, null, page, perPage);
    }

    /**
     * 商家分页。{@code onlyMerchantId} 非空时只返回该商家自己那一行（MERCHANT 角色用），
     * 避免商家枚举到全部商家档案（含联系方式）。
     */
    public Map<String, Object> findPage(String keyword, Long onlyMerchantId, int page, int perPage) {
        StringBuilder where = new StringBuilder(" WHERE is_deleted = 0");
        List<Object> args = new java.util.ArrayList<>();
        if (onlyMerchantId != null) {
            where.append(" AND id = ?");
            args.add(onlyMerchantId);
        }
        if (keyword != null && !keyword.trim().isEmpty()) {
            where.append(" AND (merchant_code LIKE ? OR merchant_name LIKE ?)");
            args.add("%" + keyword.trim() + "%");
            args.add("%" + keyword.trim() + "%");
        }
        String whereSql = where.toString();
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM merchant" + whereSql, Integer.class, args.toArray());
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT * FROM merchant" + whereSql + " ORDER BY id DESC LIMIT ? OFFSET ?",
                java.util.stream.Stream.concat(args.stream(), java.util.stream.Stream.of(perPage, (page - 1) * perPage)).toArray());
        Map<String, Object> result = new HashMap<>(4);
        result.put("count", count == null ? 0 : count);
        result.put("rows", rows);
        return result;
    }

    public List<Map<String, Object>> listAll() {
        return listAll(null);
    }

    /**
     * 商家名录。{@code onlyMerchantId} 非空时只返回该商家自己那一行（供 MERCHANT 角色使用）。
     * <p>
     * 注意 MERCHANT 分支**不套 {@code status = 1} 过滤** —— 该方法被前端当作"商家名字字典"用于
     * 下单页/商品页的必选下拉，若把已停用商家过滤掉，该商家自己登录后整页不可用。
     *
     * @param onlyMerchantId 非空则限定为单个商家；null 表示平台视角，返回全部启用商家
     */
    public List<Map<String, Object>> listAll(Long onlyMerchantId) {
        if (onlyMerchantId != null) {
            return jdbcTemplate.queryForList(
                    "SELECT id, merchant_code, merchant_name, user_id FROM merchant "
                            + "WHERE is_deleted = 0 AND id = ? ORDER BY id ASC", onlyMerchantId);
        }
        return jdbcTemplate.queryForList(
                "SELECT id, merchant_code, merchant_name, user_id FROM merchant "
                        + "WHERE is_deleted = 0 AND status = 1 ORDER BY id ASC");
    }
}
