package com.java3y.austin.agent.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 渠道路由推荐结果（Agent 7 结构化输出）
 *
 * @author sentinel
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RouteAdviceResult {

    private Long recommendedChannelId;
    private String channelCode;
    private String channelName;
    private String carrierName;
    private String type;
    private BigDecimal freight;
    private String currency;
    private Integer transitDaysMax;
    private String reason;
}
