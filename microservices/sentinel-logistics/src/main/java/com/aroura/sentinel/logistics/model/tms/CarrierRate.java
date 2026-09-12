package com.aroura.sentinel.logistics.model.tms;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 运费价卡（渠道 × 区域 × 重量段），对应 carrier_rate 表
 *
 * @author sentinel
 */
@Data
@Builder
public class CarrierRate {

    private Long id;
    private Long channelId;
    private String zone;
    private BigDecimal minWeightKg;
    private BigDecimal maxWeightKg;
    /**
     * 计费方式：PER_KG 单价 | FIRST_CONTINUED 首续重
     */
    private String mode;
    private BigDecimal firstWeightKg;
    private BigDecimal firstPrice;
    private BigDecimal continuedWeightKg;
    private BigDecimal continuedPrice;
    private BigDecimal price;
    private String currency;
    private Date effectiveFrom;
    private Date effectiveTo;
    private Integer status;
    private Long createdAt;
    private Long updatedAt;
    private Integer isDeleted;
}
