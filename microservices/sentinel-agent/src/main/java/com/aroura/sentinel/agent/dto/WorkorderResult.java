package com.aroura.sentinel.agent.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 工单处理结果（Agent 5 结构化输出）
 *
 * @author sentinel
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkorderResult {

    /**
     * 工单类型：customs_delay/lost/returned/delivery_failed
     */
    private String type;

    /**
     * 优先级：P0/P1/P2
     */
    private String level;

    /**
     * 处理 SOP
     */
    private String sop;
}
