package com.java3y.austin.logistics.model.tms;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 物流渠道（承运商 × 目的地 × 时效），对应 carrier_channel 表
 *
 * @author sentinel
 */
@Data
@Builder
public class CarrierChannel {

    private Long id;
    private Long carrierId;
    private String channelCode;
    private String channelName;
    private String type;
    private String destCountry;
    private Integer transitDaysMin;
    private Integer transitDaysMax;
    private String trackingPrefix;
    private BigDecimal minBillableWeightKg;
    private Integer volDivisor;
    private Integer status;
    private String remark;
    private Long createdAt;
    private Long updatedAt;
    private Integer isDeleted;
}
