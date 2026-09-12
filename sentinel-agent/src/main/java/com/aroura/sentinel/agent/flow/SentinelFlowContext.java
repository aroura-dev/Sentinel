package com.aroura.sentinel.agent.flow;

import lombok.Data;

/**
 * 异常处置链上下文（P1-2）
 * <p>
 * 在 chain 各节点间传递：异常识别结果、分类结果、工单号、通知结果等。
 *
 * @author sentinel
 */
@Data
public class SentinelFlowContext {
    private String orderNo;
    private String anomalyDesc;
    private String exceptionType;
    private String priority;
    private String sop;
    private String logisticsNode;
    private Long workOrderId;
    private boolean lowConfidence;
    private boolean notified;
    private boolean degraded;
    private String traceId;
}
