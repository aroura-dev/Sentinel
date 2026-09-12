package com.java3y.austin.logistics.model.tms;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 运费报价结果
 *
 * @author sentinel
 */
@Data
@Builder
public class FreightQuote {

    private BigDecimal billableWeight;
    private String zone;
    /**
     * 计费方式：PER_KG / FIRST_CONTINUED
     */
    private String mode;
    /**
     * 单价（PER_KG 为单价；FIRST_CONTINUED 为首重价，供展示）
     */
    private BigDecimal price;
    private String currency;
    /**
     * 运费金额（已四舍五入到分）
     */
    private BigDecimal freight;
    private Long rateId;
    private String channelCode;
    private String channelName;
}
