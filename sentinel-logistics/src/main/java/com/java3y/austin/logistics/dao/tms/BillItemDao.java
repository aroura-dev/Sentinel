package com.java3y.austin.logistics.dao.tms;

import com.java3y.austin.logistics.model.tms.BillItem;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 账单明细 DAO
 *
 * @author sentinel
 */
@Repository
public class BillItemDao {

    private final JdbcTemplate jdbcTemplate;

    public BillItemDao(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Long insert(BillItem item) {
        jdbcTemplate.update(
                "INSERT INTO bill_item (bill_id, waybill_id, waybill_no, order_no, merchant_id, tracking_no, weight_kg, billable_weight_kg, "
                        + "freight_cost, currency, billed_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                item.getBillId(), item.getWaybillId(), item.getWaybillNo(), item.getOrderNo(), item.getMerchantId(),
                item.getTrackingNo(), item.getWeightKg(), item.getBillableWeightKg(), item.getFreightCost(),
                item.getCurrency(), item.getBilledAt());
        Number key = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Number.class);
        return key == null ? null : key.longValue();
    }

    public List<Map<String, Object>> findByBillId(Long billId) {
        return jdbcTemplate.queryForList(
                "SELECT * FROM bill_item WHERE bill_id = ? AND is_deleted = 0 ORDER BY id ASC", billId);
    }

    public List<Map<String, Object>> listByMerchantId(Long merchantId) {
        return jdbcTemplate.queryForList(
                "SELECT * FROM bill_item WHERE merchant_id = ? AND is_deleted = 0 ORDER BY id DESC LIMIT 200", merchantId);
    }

    public Map<String, Object> statsByMerchant(Long merchantId) {
        List<Map<String, Object>> list = jdbcTemplate.queryForList(
                "SELECT bill.status, COUNT(*) AS cnt, COALESCE(SUM(bill_item.freight_cost), 0) AS amount "
                        + "FROM bill_item LEFT JOIN bill ON bill_item.bill_id = bill.id "
                        + "WHERE bill_item.merchant_id = ? AND bill_item.is_deleted = 0 AND bill.is_deleted = 0 "
                        + "GROUP BY bill.status", merchantId);
        Map<String, Object> result = new HashMap<>(4);
        BigDecimalOpen total = new BigDecimalOpen();
        for (Map<String, Object> row : list) {
            String status = row.get("status") == null ? "DRAFT" : String.valueOf(row.get("status"));
            result.put(status.toLowerCase(), row);
            if ("SUBMITTED".equals(status) || "VERIFIED".equals(status) || "SETTLED".equals(status)) {
                total.add(row.get("amount"));
            }
        }
        result.put("total_billed", total.getValue());
        return result;
    }

    private static final class BigDecimalOpen {
        private java.math.BigDecimal value = java.math.BigDecimal.ZERO;

        void add(Object v) {
            if (v != null) {
                value = value.add(new java.math.BigDecimal(String.valueOf(v)));
            }
        }

        java.math.BigDecimal getValue() {
            return value;
        }
    }
}
