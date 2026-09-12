package com.aroura.sentinel.logistics.dao.tms;

import com.aroura.sentinel.logistics.model.tms.Bill;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 承运商账单 DAO
 *
 * @author sentinel
 */
@Repository
public class BillDao {

    private final JdbcTemplate jdbcTemplate;

    public BillDao(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Long insert(Bill b) {
        jdbcTemplate.update(
                "INSERT INTO bill (bill_no, carrier_id, period_start, period_end, currency, total_amount, status, remark) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                b.getBillNo(), b.getCarrierId(), b.getPeriodStart(), b.getPeriodEnd(), b.getCurrency(),
                b.getTotalAmount(), b.getStatus(), b.getRemark());
        Number key = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Number.class);
        return key == null ? null : key.longValue();
    }

    public Map<String, Object> findById(Long id) {
        List<Map<String, Object>> list = jdbcTemplate.queryForList(
                "SELECT * FROM bill WHERE id = ? AND is_deleted = 0", id);
        return list.isEmpty() ? null : list.get(0);
    }

    public Map<String, Object> findByCarrierPeriod(Long carrierId, Date periodStart, Date periodEnd) {
        List<Map<String, Object>> list = jdbcTemplate.queryForList(
                "SELECT * FROM bill WHERE carrier_id = ? AND period_start = ? AND period_end = ? AND is_deleted = 0 LIMIT 1",
                carrierId, periodStart, periodEnd);
        return list.isEmpty() ? null : list.get(0);
    }

    public void updateStatus(Bill b) {
        jdbcTemplate.update(
                "UPDATE bill SET status=?, submitted_by=?, submitted_at=?, verified_by=?, verified_at=?, settled_by=?, settled_at=?, "
                        + "rejected_by=?, rejected_at=?, reject_reason=? WHERE id=? AND is_deleted=0",
                b.getStatus(), b.getSubmittedBy(), b.getSubmittedAt(), b.getVerifiedBy(), b.getVerifiedAt(),
                b.getSettledBy(), b.getSettledAt(), b.getRejectedBy(), b.getRejectedAt(), b.getRejectReason(), b.getId());
    }

    public void updateTotalAmount(Long id, java.math.BigDecimal totalAmount) {
        jdbcTemplate.update("UPDATE bill SET total_amount = ? WHERE id = ? AND is_deleted = 0", totalAmount, id);
    }

    public Map<String, Object> findPage(String status, Long carrierId, int page, int perPage) {
        StringBuilder where = new StringBuilder(" WHERE is_deleted = 0");
        List<Object> args = new java.util.ArrayList<>();
        if (status != null && !status.trim().isEmpty()) {
            where.append(" AND status = ?");
            args.add(status.trim());
        }
        if (carrierId != null) {
            where.append(" AND carrier_id = ?");
            args.add(carrierId);
        }
        String whereSql = where.toString();
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM bill" + whereSql, Integer.class, args.toArray());
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT * FROM bill" + whereSql + " ORDER BY id DESC LIMIT ? OFFSET ?",
                java.util.stream.Stream.concat(args.stream(), java.util.stream.Stream.of(perPage, (page - 1) * perPage)).toArray());
        Map<String, Object> result = new HashMap<>(4);
        result.put("count", count == null ? 0 : count);
        result.put("rows", rows);
        return result;
    }

    public Map<String, Object> stats() {
        Map<String, Object> result = new HashMap<>(4);
        result.put("total", jdbcTemplate.queryForObject("SELECT COUNT(*) FROM bill WHERE is_deleted = 0", Integer.class));
        result.put("draft", jdbcTemplate.queryForObject("SELECT COUNT(*) FROM bill WHERE is_deleted = 0 AND status = 'DRAFT'", Integer.class));
        result.put("submitted", jdbcTemplate.queryForObject("SELECT COUNT(*) FROM bill WHERE is_deleted = 0 AND status = 'SUBMITTED'", Integer.class));
        result.put("settled", jdbcTemplate.queryForObject("SELECT COUNT(*) FROM bill WHERE is_deleted = 0 AND status = 'SETTLED'", Integer.class));
        return result;
    }
}
