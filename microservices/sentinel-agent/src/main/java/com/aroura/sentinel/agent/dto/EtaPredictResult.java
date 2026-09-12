package com.aroura.sentinel.agent.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 时效预测结果（Agent 8 结构化输出）
 *
 * @author sentinel
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EtaPredictResult {

    private String orderNo;
    private String node;
    private Integer remainingDays;
    private String etaDate;
    private String confidence;
    private String reason;
}
