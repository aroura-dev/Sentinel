package com.java3y.austin.web.controller.sentinel;


import com.java3y.austin.agent.service.AgentCallLogService;
import com.java3y.austin.common.vo.BasicResultVO;
import com.java3y.austin.web.annotation.RequireRole;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Agent 调用日志接口（PRD 8.4）
 * <p>
 * 修复：原接口仅存在于前端 Mock，后端补齐；数据来自 agent_call_log 表。
 *
 * @author sentinel
 */
@RestController
@RequestMapping("/api/agent/log")
@Api(tags = "Sentinel Agent 日志接口")
@RequireRole({"ADMIN"})
public class SentinelAgentLogController {

    @Autowired
    private AgentCallLogService agentCallLogService;

    @GetMapping("/stats")
    @ApiOperation("Agent 调用统计")
    public BasicResultVO stats() {
        return BasicResultVO.success(agentCallLogService.stats());
    }

    @GetMapping("/list")
    @ApiOperation("Agent 日志分页")
    public BasicResultVO list(@RequestParam(required = false) String agentName,
                              @RequestParam(required = false) String traceId,
                              @RequestParam(required = false) String status,
                              @RequestParam(defaultValue = "1") Integer page,
                              @RequestParam(defaultValue = "10") Integer perPage) {
        return BasicResultVO.success(agentCallLogService.queryPage(agentName, traceId, status, page, perPage));
    }

    @GetMapping("/{id}")
    @ApiOperation("Agent 日志详情")
    public BasicResultVO detail(@PathVariable Long id) {
        Map<String, Object> log = agentCallLogService.queryById(id);
        return log == null ? BasicResultVO.fail("日志不存在") : BasicResultVO.success(log);
    }

    @GetMapping("/trace")
    @ApiOperation("按 traceId 查询链路（空 traceId 兼容，避免被 /{id} 拦截）")
    public BasicResultVO traceEmpty() {
        return BasicResultVO.success(java.util.Collections.emptyList());
    }

    @GetMapping("/trace/latest")
    @ApiOperation("最新可追溯链路 TraceId（多步链路优先）")
    public BasicResultVO traceLatest() {
        Map<String, Object> result = new HashMap<>(2);
        result.put("traceId", agentCallLogService.latestTraceId());
        return BasicResultVO.success(result);
    }
    @GetMapping("/trace/{traceId}")
    @ApiOperation("按 traceId 查询链路")
    public BasicResultVO trace(@PathVariable String traceId) {
        List<Map<String, Object>> list = agentCallLogService.queryByTraceId(traceId);
        return BasicResultVO.success(list);
    }
}