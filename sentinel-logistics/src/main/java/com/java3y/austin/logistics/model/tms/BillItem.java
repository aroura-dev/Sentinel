package com.java3y.austin.logistics.model.tms;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 账单明细（每运单一行），对应 bill_item 表
 *
 * @author sentinel
 */
@Data
@Builder
public class BillItem {

    private Long id;
    private Long billId;
    private Long waybillId;
    private String waybillNo;
    private String orderNo;
    private Long merchantId;
    private String trackingNo;
    private BigDecimal weightKg;
    private BigDecimal billableWeightKg;
    private BigDecimal freightCost;
    private String currency;
    private Date billedAt;
    private Long createdAt;
    private Long updatedAt;
    private Integer isDeleted;
}
