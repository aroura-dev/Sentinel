package com.java3y.austin.agent.service;

import com.alibaba.fastjson2.JSON;
import com.java3y.austin.agent.dao.AgentCallLogDao;
import com.java3y.austin.agent.model.AgentCallLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Agent 调用日志记录服务
 * <p>
 * 修复：原实现仅输出日志未入库，导致 agent_call_log 表为空、无法按 trace_id 追溯。
 * 现改为：每次调用写入 agent_call_log 表；入库失败不影响 Agent 主流程（try/catch + 日志）。
 *
 * @author sentinel
 */
@Service
public class AgentCallLogService {

    private static final Logger log = LoggerFactory.getLogger(AgentCallLogService.class);

    @Autowired
    private AgentCallLogDao agentCallLogDao;

    /**
     * 生成 trace_id（用于关联同一通知的所有 Agent 调用）
     */
    public String generateTraceId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * 记录 Agent 调用（写入 agent_call_log 表）
     *
     * @param agentName   Agent 名称
     * @param input       输入参数
     * @param output      输出结果
     * @param toolsCalled 调用的 Tool 列表
     * @param tokenUsage  Token 消耗
     * @param latencyMs   耗时（毫秒）
     * @param status      状态：success/failed/timeout/degraded
     * @param traceId     链路 ID
     */
    public void record(String agentName, Object input, Object output,
                       String toolsCalled, Integer tokenUsage, long latencyMs, String status, String traceId) {
        AgentCallLog callLog = AgentCallLog.builder()
                .agentName(agentName)
                .input(input == null ? null : JSON.toJSONString(input))
                .output(output == null ? null : JSON.toJSONString(output))
                .toolsCalled(toolsCalled)
                .tokenUsage(tokenUsage)
                .latencyMs((int) latencyMs)
                .status(status)
                .traceId(traceId)
                .createdAt(System.currentTimeMillis())
                .build();
        log.info("[AgentCall] {} traceId={} status={} latencyMs={}ms", agentName, traceId, status, latencyMs);
        try {
            agentCallLogDao.insert(callLog);
        } catch (Exception e) {
            // 日志入库失败不影响 Agent 主流程
            log.error("[AgentCallLog] 日志入库失败 agentName={} traceId={}", agentName, traceId, e);
        }
    }

    /**
     * 分页查询（供 /api/agent/log/list 使用）
     */
    public Map<String, Object> queryPage(String agentName, String traceId, String status, int page, int perPage) {
        return agentCallLogDao.queryPage(agentName, traceId, status, page, perPage);
    }

    /**
     * 按 id 查询（供 /api/agent/log/{id} 使用）
     */
    public Map<String, Object> queryById(Long id) {
        return agentCallLogDao.queryById(id);
    }

    /**
     * 按 traceId 查询链路（供 /api/agent/log/trace/{traceId} 使用）
     */
    public List<Map<String, Object>> queryByTraceId(String traceId) {
        return agentCallLogDao.queryByTraceId(traceId);
    }

    /**
     * 统计（供 /api/agent/log/stats 使用）
     */
    /**
     * 最新可追溯链路 TraceId（优先返回多步链路，供链路追踪页默认展示）
     */
    public String latestTraceId() {
        return agentCallLogDao.latestTraceChainId();
    }
    public Map<String, Object> stats() {
        return agentCallLogDao.stats();
    }
}