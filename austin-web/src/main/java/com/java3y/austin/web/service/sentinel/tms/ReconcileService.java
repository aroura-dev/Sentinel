package com.java3y.austin.web.service.sentinel.tms;

import com.java3y.austin.logistics.dao.LogisticsDao;
import com.java3y.austin.logistics.dao.tms.BillDao;
import com.java3y.austin.logistics.dao.tms.BillItemDao;
import com.java3y.austin.logistics.dao.tms.WaybillDao;
import com.java3y.austin.logistics.engine.FreightCalculator;
import com.java3y.austin.logistics.engine.FreightNoRateException;
import com.java3y.austin.logistics.model.tms.FreightQuote;
import com.java3y.austin.web.exception.CommonException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 差异对账服务：系统已入账金额 vs 按当前价卡重算（承运商口径）
 * <p>无独立承运商账单表，用 {@link FreightCalculator} 重算近似；标记差异原因写操作审计。
 *
 * @author sentinel
 */
@Service
public class ReconcileService {

    private static final String METRIC_NOTE =
            "系统金额取账单入账运费快照；承运商口径按当前价卡重算；差异 = 承运商金额 - 系统金额";

    private final BillDao billDao;
    private final BillItemDao billItemDao;
    private final WaybillDao waybillDao;
    private final LogisticsDao logisticsDao;
    private final FreightCalculator freightCalculator;
    private final AuditLogService auditLogService;

    public ReconcileService(BillDao billDao, BillItemDao billItemDao, WaybillDao waybillDao,
                            LogisticsDao logisticsDao, FreightCalculator freightCalculator,
                            AuditLogService auditLogService) {
        this.billDao = billDao;
        this.billItemDao = billItemDao;
        this.waybillDao = waybillDao;
        this.logisticsDao = logisticsDao;
        this.freightCalculator = freightCalculator;
        this.auditLogService = auditLogService;
    }

    public Map<String, Object> bills() {
        return billDao.findPage(null, null, 1, 100);
    }

    public Map<String, Object> reconcile(Long billId) {
        Map<String, Object> bill = billDao.findById(billId);
        if (bill == null) {
            throw new CommonException("账单不存在: " + billId);
        }
        List<Map<String, Object>> items = billItemDao.findByBillId(billId);
        List<Map<String, Object>> rows = new ArrayList<>();
        BigDecimal sysTotal = BigDecimal.ZERO;
        BigDecimal expectedTotal = BigDecimal.ZERO;
        int diffCount = 0;
        for (Map<String, Object> it : items) {
            BigDecimal sys = dec(it.get("freight_cost"));
            BigDecimal expected = null;
            String note = null;
            Map<String, Object> wb = waybillDao.findByWaybillNo(String.valueOf(it.get("waybill_no")));
            if (wb != null) {
                Map<String, Object> order = logisticsDao.findOrderByNo(String.valueOf(wb.get("order_no")));
                String dest = order == null ? null : String.valueOf(order.get("destination_country"));
                try {
                    FreightQuote q = freightCalculator.quote(
                            toLong(wb.get("channel_id")), dec(wb.get("weight_kg")), dec(wb.get("volume_l")),
                            dest == null ? "" : dest);
                    expected = q.getFreight();
                } catch (FreightNoRateException e) {
                    note = "当前无价卡";
                }
            }
            BigDecimal diff = expected == null ? null : expected.subtract(sys);
            sysTotal = sysTotal.add(sys);
            if (expected != null) {
                expectedTotal = expectedTotal.add(expected);
            }
            if (diff != null && diff.compareTo(BigDecimal.ZERO) != 0) {
                diffCount++;
            }
            Map<String, Object> row = new HashMap<>(8);
            row.put("waybillNo", it.get("waybill_no"));
            row.put("orderNo", it.get("order_no"));
            row.put("sys", sys);
            row.put("expected", expected);
            row.put("diff", diff);
            row.put("diffStatus", diff == null ? "NA"
                    : diff.compareTo(BigDecimal.ZERO) > 0 ? "HIGHER"
                    : diff.compareTo(BigDecimal.ZERO) < 0 ? "LOWER" : "EQUAL");
            row.put("note", note);
            rows.add(row);
        }
        Map<String, Object> result = new HashMap<>(8);
        result.put("bill", bill);
        result.put("rows", rows);
        result.put("sysTotal", sysTotal.setScale(2));
        result.put("expectedTotal", expectedTotal.setScale(2));
        result.put("diffTotal", expectedTotal.subtract(sysTotal).setScale(2));
        result.put("diffCount", diffCount);
        result.put("metricNote", METRIC_NOTE);
        return result;
    }

    public Map<String, Object> mark(Long billId, String waybillNo, String reason) {
        if (waybillNo == null || waybillNo.trim().isEmpty()) {
            throw new CommonException("运单号不能为空");
        }
        if (reason == null || reason.trim().isEmpty()) {
            throw new CommonException("差异原因不能为空");
        }
        auditLogService.log("reconcile", "MARK", String.valueOf(billId),
                "运单 " + waybillNo.trim() + " 标差异原因: " + reason.trim());
        Map<String, Object> result = new HashMap<>(4);
        result.put("billId", billId);
        result.put("waybillNo", waybillNo.trim());
        result.put("reason", reason.trim());
        return result;
    }

    private static BigDecimal dec(Object v) {
        return v == null ? BigDecimal.ZERO : new BigDecimal(String.valueOf(v));
    }

    private static Long toLong(Object v) {
        return v == null ? null : Long.valueOf(String.valueOf(v));
    }
}
