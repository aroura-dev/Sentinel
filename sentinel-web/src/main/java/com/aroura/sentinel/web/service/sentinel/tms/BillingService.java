package com.aroura.sentinel.web.service.sentinel.tms;

import com.aroura.sentinel.logistics.dao.tms.BillDao;
import com.aroura.sentinel.logistics.dao.tms.BillItemDao;
import com.aroura.sentinel.logistics.dao.tms.WaybillDao;
import com.aroura.sentinel.logistics.engine.BillingEngine;
import com.aroura.sentinel.logistics.enums.BillStatus;
import com.aroura.sentinel.logistics.model.tms.Bill;
import com.aroura.sentinel.logistics.model.tms.BillItem;
import com.aroura.sentinel.web.exception.CommonException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * 计费结算服务：账单生成 + 状态机流转（DRAFT→SUBMITTED→VERIFIED→SETTLED，可驳回）
 *
 * @author sentinel
 */
@Service
public class BillingService {

    private static final SimpleDateFormat DATE_FMT = new SimpleDateFormat("yyyy-MM-dd");
    private static final String CNY = "CNY";

    private final BillingEngine billingEngine;
    private final BillDao billDao;
    private final BillItemDao billItemDao;
    private final WaybillDao waybillDao;
    private final AuditLogService auditLogService;

    public BillingService(BillingEngine billingEngine, BillDao billDao, BillItemDao billItemDao,
                          WaybillDao waybillDao, AuditLogService auditLogService) {
        this.billingEngine = billingEngine;
        this.billDao = billDao;
        this.billItemDao = billItemDao;
        this.waybillDao = waybillDao;
        this.auditLogService = auditLogService;
    }

    /**
     * 手动入账：把一单未入账运单加入 DRAFT 账单（要求承运商一致）
     */
    public Map<String, Object> addWaybill(Long billId, String waybillNo) {
        Map<String, Object> bill = billDao.findById(billId);
        if (bill == null || !"DRAFT".equals(String.valueOf(bill.get("status")))) {
            throw new CommonException("账单不存在或非草稿状态");
        }
        Map<String, Object> wb = waybillDao.findByWaybillNo(waybillNo);
        if (wb == null) {
            throw new CommonException("运单不存在: " + waybillNo);
        }
        if (Integer.valueOf(1).equals(toInt(wb.get("billed")))) {
            throw new CommonException("运单已入账");
        }
        if (!String.valueOf(bill.get("carrier_id")).equals(String.valueOf(wb.get("carrier_id")))) {
            throw new CommonException("运单承运商与账单不一致");
        }
        BigDecimal freight = (BigDecimal) wb.get("freight_cost");
        BillItem item = BillItem.builder()
                .billId(billId)
                .waybillId(((Number) wb.get("id")).longValue())
                .waybillNo(waybillNo)
                .orderNo(String.valueOf(wb.get("order_no")))
                .merchantId(wb.get("merchant_id") == null ? null : ((Number) wb.get("merchant_id")).longValue())
                .trackingNo(String.valueOf(wb.get("tracking_no")))
                .weightKg((BigDecimal) wb.get("weight_kg"))
                .billableWeightKg((BigDecimal) wb.get("billable_weight_kg"))
                .freightCost(freight)
                .currency(wb.get("freight_currency") == null ? CNY : String.valueOf(wb.get("freight_currency")))
                .billedAt(new Date())
                .build();
        billItemDao.insert(item);
        waybillDao.markBilled(((Number) wb.get("id")).longValue());

        BigDecimal oldTotal = new BigDecimal(String.valueOf(bill.get("total_amount")));
        billDao.updateTotalAmount(billId, oldTotal.add(freight));

        return detail(billId);
    }

    public Map<String, Object> generate(Long carrierId, String periodStart, String periodEnd) {
        Date ps = parseDate(periodStart);
        Date pe = parseDate(periodEnd);
        if (ps == null || pe == null || ps.after(pe)) {
            throw new CommonException("账期非法");
        }
        Map<String, Object> result = billingEngine.generate(carrierId, ps, pe);
        if (result == null) {
            throw new CommonException("无运单可入账，或该承运商账期账单已存在");
        }
        Object billNo = result.get("bill") == null ? null
                : String.valueOf(((Map<?, ?>) result.get("bill")).get("bill_no"));
        auditLogService.log("bill", "GENERATE", billNo == null ? null : String.valueOf(billNo),
                "生成账单 承运商ID=" + carrierId + " 账期=" + periodStart + "~" + periodEnd);
        return result;
    }

    /**
     * 状态机流转：submit/verify/settle/reject/reopen
     */
    public Map<String, Object> transition(Long billId, String action, String operator, String reason) {
        Map<String, Object> row = billDao.findById(billId);
        if (row == null) {
            throw new CommonException("账单不存在");
        }
        BillStatus from = BillStatus.valueOf(String.valueOf(row.get("status")));
        BillStatus target = targetOf(action);
        if (target == null || !BillStatus.canTransition(from, target)) {
            throw new CommonException("非法状态流转: " + from.getCode() + " -> " + (target == null ? action : target.getCode()));
        }
        Date now = new Date();
        Bill b = Bill.builder()
                .id(billId)
                .status(target.getCode())
                .build();
        switch (target) {
            case SUBMITTED:
                b.setSubmittedBy(operator);
                b.setSubmittedAt(now);
                break;
            case VERIFIED:
                b.setVerifiedBy(operator);
                b.setVerifiedAt(now);
                break;
            case SETTLED:
                b.setSettledBy(operator);
                b.setSettledAt(now);
                break;
            case REJECTED:
                b.setRejectedBy(operator);
                b.setRejectedAt(now);
                b.setRejectReason(reason);
                break;
            case DRAFT:
                // 重开：清空各阶段操作人/时间，保留 bill_item
                b.setSubmittedBy(null);
                b.setSubmittedAt(null);
                b.setVerifiedBy(null);
                b.setVerifiedAt(null);
                b.setSettledBy(null);
                b.setSettledAt(null);
                b.setRejectedBy(null);
                b.setRejectedAt(null);
                b.setRejectReason(null);
                break;
            default:
                break;
        }
        billDao.updateStatus(b);
        auditLogService.log("bill", action.toUpperCase(), String.valueOf(row.get("bill_no")),
                "账单状态 " + from.getCode() + " → " + target.getCode()
                        + (reason == null ? "" : " 原因=" + reason));
        return billDao.findById(billId);
    }

    public Map<String, Object> list(String status, Long carrierId, int page, int perPage) {
        return billDao.findPage(status, carrierId, page, perPage);
    }

    public Map<String, Object> detail(Long id) {
        Map<String, Object> bill = billDao.findById(id);
        if (bill == null) {
            throw new CommonException("账单不存在");
        }
        Map<String, Object> result = new HashMap<>(4);
        result.put("bill", bill);
        result.put("items", billItemDao.findByBillId(id));
        return result;
    }

    public Map<String, Object> stats() {
        return billDao.stats();
    }

    /**
     * 当前商家账单汇总（bill_item 行级透视）
     */
    public Map<String, Object> merchantSelf(Long merchantId) {
        Map<String, Object> result = new HashMap<>(4);
        result.put("items", billItemDao.listByMerchantId(merchantId));
        result.put("stats", billItemDao.statsByMerchant(merchantId));
        return result;
    }

    private BillStatus targetOf(String action) {
        if (action == null) {
            return null;
        }
        switch (action) {
            case "submit":
                return BillStatus.SUBMITTED;
            case "verify":
                return BillStatus.VERIFIED;
            case "settle":
                return BillStatus.SETTLED;
            case "reject":
                return BillStatus.REJECTED;
            case "reopen":
                return BillStatus.DRAFT;
            default:
                return null;
        }
    }

    private Date parseDate(String v) {
        if (v == null || v.trim().isEmpty()) {
            return null;
        }
        try {
            return DATE_FMT.parse(v);
        } catch (Exception e) {
            return null;
        }
    }

    private static Integer toInt(Object v) {
        return v == null ? null : Integer.valueOf(String.valueOf(v));
    }
}
