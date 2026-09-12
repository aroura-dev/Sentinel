package com.java3y.austin.agent.model;

import com.alibaba.fastjson2.annotation.JSONField;
import lombok.Builder;
import lombok.Data;

/**
 * Agent 调用日志（对应 agent_call_log 表）
 * <p>
 * 每次 LLM 调用记录到该表，支持按 trace_id 全链路追溯。
 * <p>
 * 注意：Web 层使用 Fastjson 输出，故用 @JSONField 显式指定 snake_case 字段名
 * （agent_name/trace_id/tools_called...），与全站 JdbcTemplate map 风格及前端取值一致。
 *
 * @author sentinel
 */
@Data
@Builder
public class AgentCallLog {

    private Long id;
    /**
     * Agent 名称（如 ContentGenAgent）
     */
    @JSONField(name = "agent_name")
    private String agentName;
    /**
     * 输入参数（JSON）
     */
    private String input;
    /**
     * 输出结果（JSON）
     */
    private String output;
    /**
     * 调用的 Tool 列表（逗号分隔）
     */
    @JSONField(name = "tools_called")
    private String toolsCalled;
    /**
     * Token 消耗
     */
    @JSONField(name = "token_usage")
    private Integer tokenUsage;
    /**
     * 耗时（毫秒）
     */
    @JSONField(name = "latency_ms")
    private Integer latencyMs;
    /**
     * 状态：success/failed/timeout/degraded
     */
    private String status;
    /**
     * 链路 ID（关联 notification_record.trace_id）
     */
    @JSONField(name = "trace_id")
    private String traceId;
    @JSONField(name = "created_at")
    private Long createdAt;
    @JSONField(name = "updated_at")
    private Long updatedAt;
}