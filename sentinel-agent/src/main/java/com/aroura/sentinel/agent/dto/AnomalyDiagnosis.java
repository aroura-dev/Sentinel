package com.aroura.sentinel.agent.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 异常诊断结果（Agent 2 结构化输出）
 *
 * @author sentinel
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnomalyDiagnosis {

    /**
     * 异常原因（自然语言）
     */
    private String reason;

    /**
     * 处理建议
     */
    private String suggestion;

    /**
     * 优先级：P0/P1/P2
     */
    private String priority;
}
