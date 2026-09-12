package com.java3y.austin.logistics.engine;

import com.java3y.austin.logistics.dao.tms.BillDao;
import com.java3y.austin.logistics.dao.tms.BillItemDao;
import com.java3y.austin.logistics.dao.tms.CarrierDao;
import com.java3y.austin.logistics.dao.tms.WaybillDao;
import com.java3y.austin.logistics.model.tms.Bill;
import com.java3y.austin.logistics.model.tms.BillItem;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 账单引擎：按 承运商 × 账期 生成账单（在途也计费，出库时间入账）
 * <p>
 * 幂等：同账期账单已存在则返回空；无运单返回空结果。
 *
 * @author sentinel
 */
@Component
public class BillingEngine {

    private static final SimpleDateFormat BILL_NO_FMT = new SimpleDateFormat("yyyyMMdd");
    private static final String CNY = "CNY";

    private final WaybillDao waybillDao;
    private final BillDao billDao;
    private final BillItemDao billItemDao;
    private final CarrierDao carrierDao;

    public BillingEngine(WaybillDao waybillDao, BillDao billDao, BillItemDao billItemDao, CarrierDao carrierDao) {
        this.waybillDao = waybillDao;
        this.billDao = billDao;
        this.billItemDao = billItemDao;
        this.carrierDao = carrierDao;
    }

    /**
     * @return 生成的账单（含 items 数/总额）；同账期已存在或无运单时返回 null
     */
    public Map<String, Object> generate(Long carrierId, Date periodStart, Date periodEnd) {
        Map<String, Object> existing = billDao.findByCarrierPeriod(carrierId, periodStart, periodEnd);
        if (existing != null) {
            return null;
        }
        List<Map<String, Object>> waybills = waybillDao.listUnbilled(carrierId, periodStart, periodEnd);
        if (waybills.isEmpty()) {
            return null;
        }

        BigDecimal total = BigDecimal.ZERO;
        for (Map<String, Object> w : waybills) {
            total = total.add((BigDecimal) w.get("freight_cost"));
        }

        Map<String, Object> carrier = carrierDao.findById(carrierId);
        String carrierCode = carrier == null ? "CARR" : String.valueOf(carrier.get("carrier_code"));
        // 账单号含起止账期（到天），保证同承运商不同账期生成的账单号不冲突（uk_bill_no 唯一）
        String billNo = "BILL-" + carrierCode + "-" + BILL_NO_FMT.format(periodStart) + "-" + BILL_NO_FMT.format(periodEnd);

        Bill bill = Bill.builder()
                .billNo(billNo)
                .carrierId(carrierId)
                .periodStart(periodStart)
                .periodEnd(periodEnd)
                .currency(CNY)
                .totalAmount(total)
                .status("DRAFT")
                .build();
        Long billId = billDao.insert(bill);

        for (Map<String, Object> w : waybills) {
            BillItem item = BillItem.builder()
                    .billId(billId)
                    .waybillId(((Number) w.get("id")).longValue())
                    .waybillNo(String.valueOf(w.get("waybill_no")))
                    .orderNo(String.valueOf(w.get("order_no")))
                    .merchantId(w.get("merchant_id") == null ? null : ((Number) w.get("merchant_id")).longValue())
                    .trackingNo(String.valueOf(w.get("tracking_no")))
                    .weightKg((BigDecimal) w.get("weight_kg"))
                    .billableWeightKg((BigDecimal) w.get("billable_weight_kg"))
                    .freightCost((BigDecimal) w.get("freight_cost"))
                    .currency(w.get("freight_currency") == null ? CNY : String.valueOf(w.get("freight_currency")))
                    .billedAt(new Date())
                    .build();
            billItemDao.insert(item);
            waybillDao.markBilled(((Number) w.get("id")).longValue());
        }

        Map<String, Object> result = new HashMap<>(4);
        result.put("bill", billDao.findById(billId));
        result.put("itemCount", waybills.size());
        result.put("totalAmount", total);
        return result;
    }
}
