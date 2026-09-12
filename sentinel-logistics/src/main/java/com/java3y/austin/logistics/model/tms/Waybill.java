package com.java3y.austin.logistics.model.tms;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 运单（出库生成，运费快照），对应 waybill 表
 *
 * @author sentinel
 */
@Data
@Builder
public class Waybill {

    private Long id;
    private String waybillNo;
    private String orderNo;
    private Long merchantId;
    private Long channelId;
    private Long carrierId;
    private String trackingNo;
    private String carrierCode;
    private BigDecimal weightKg;
    private BigDecimal volumeL;
    private BigDecimal billableWeightKg;
    private BigDecimal declaredValue;
    private String declaredCurrency;
    private BigDecimal freightCost;
    private String freightCurrency;
    private String zone;
    /**
     * 商品明细 JSON（整单出库=订单全量；分批出库=本次出库子集；合并运单=各单合并）
     */
    private String itemsJson;
    private Date promiseEta;
    private Date actualDeliveredAt;
    /**
     * 状态：ACTIVE/DELIVERED/CANCELED
     */
    private String status;
    private Integer billed;
    private Long createdAt;
    private Long updatedAt;
    private Integer isDeleted;
}
