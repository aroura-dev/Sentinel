package com.aroura.sentinel.web.service.sentinel.tms;

import com.aroura.sentinel.logistics.dao.tms.BillDao;
import com.aroura.sentinel.logistics.dao.tms.BillItemDao;
import com.aroura.sentinel.logistics.dao.tms.WaybillDao;
import com.aroura.sentinel.logistics.engine.BillingEngine;
import com.aroura.sentinel.logistics.model.tms.BillItem;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 验证多步写库的失败不会被吞掉 —— 这是 Spring 能够回滚的前提。
 * <p>
 * {@code addWaybill} 连写三次：插账单明细 → 标记运单已计费 → 更新账单总额。
 * 第三步失败时，若异常被方法内部捕获，前两次写就提交了，结果是"账单明细在、总额没加"，
 * 这笔钱永远计不进账单、也无法对账。本测试锁住"异常必须向外抛"这一点。
 * <p>
 * 真正的回滚由 {@code @Transactional} 保证，纯单测跑不了真库；此处验证的是那个前提条件。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BillingServiceTransactionTest {

    @Mock
    private BillingEngine billingEngine;
    @Mock
    private BillDao billDao;
    @Mock
    private BillItemDao billItemDao;
    @Mock
    private WaybillDao waybillDao;
    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private BillingService billingService;

    private Map<String, Object> draftBill() {
        Map<String, Object> bill = new HashMap<>();
        bill.put("id", 1L);
        bill.put("status", "DRAFT");
        bill.put("carrier_id", 7L);
        bill.put("total_amount", new BigDecimal("100.00"));
        return bill;
    }

    private Map<String, Object> unbilledWaybill() {
        Map<String, Object> wb = new HashMap<>();
        wb.put("id", 42L);
        wb.put("order_no", "OMT-SEED-0001");
        wb.put("tracking_no", "TRK-1");
        wb.put("carrier_id", 7L);
        wb.put("merchant_id", 1L);
        wb.put("billed", 0);
        wb.put("freight_cost", new BigDecimal("20.00"));
        wb.put("weight_kg", new BigDecimal("2.0"));
        wb.put("billable_weight_kg", new BigDecimal("2.5"));
        wb.put("freight_currency", "CNY");
        return wb;
    }

    @Test
    void updateTotalAmountFails_throwsOutOfMethodSoTransactionCanRollBack() {
        when(billDao.findById(1L)).thenReturn(draftBill());
        when(waybillDao.findByWaybillNo("WB-1")).thenReturn(unbilledWaybill());
        doThrow(new IllegalStateException("模拟更新账单总额失败"))
                .when(billDao).updateTotalAmount(anyLong(), any(BigDecimal.class));

        // 前两次写已执行，第三次抛异常 —— 异常必须一路抛出去，不能被方法吞掉
        assertThrows(IllegalStateException.class, () -> billingService.addWaybill(1L, "WB-1"));

        verify(billItemDao).insert(any(BillItem.class));
        verify(waybillDao).markBilled(42L);
    }

    @Test
    void businessValidationFailure_propagatesBeforeAnyWrite() {
        when(billDao.findById(1L)).thenReturn(draftBill());
        when(waybillDao.findByWaybillNo("WB-1")).thenReturn(unbilledWaybill());
        Map<String, Object> billed = unbilledWaybill();
        billed.put("billed", 1);
        when(waybillDao.findByWaybillNo("WB-2")).thenReturn(billed);

        assertThrows(RuntimeException.class, () -> billingService.addWaybill(1L, "WB-2"));
    }
}
