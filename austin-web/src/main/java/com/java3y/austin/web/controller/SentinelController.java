package com.java3y.austin.web.controller;

import com.alibaba.fastjson2.JSONObject;
import com.java3y.austin.agent.agent.ContentGenAgent;
import com.java3y.austin.agent.agent.CsRouteAgent;
import com.java3y.austin.agent.agent.AnomalyDiagnoseAgent;
import com.java3y.austin.agent.agent.WorkorderAgent;
import com.java3y.austin.agent.flow.SentinelFlowConfig;
import com.java3y.austin.agent.flow.SentinelFlowContext;
import com.java3y.austin.agent.flow.SentinelFlowRuleService;
import com.java3y.austin.agent.flow.SentinelFlowService;
import com.java3y.austin.agent.service.AgentCallLogService;
import com.java3y.austin.common.vo.BasicResultVO;
import com.java3y.austin.web.annotation.RequireRole;
import com.java3y.austin.web.service.CsSessionService;
import com.java3y.austin.web.service.sentinel.tms.AuditLogService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Sentinel Agent 对外接口
 * <p>
 * 提供 6 个 Agent 的直接调用接口，用于前端测试和演示。
 *
 * @author sentinel
 */
@RestController
@RequestMapping("/api/agent")
@Api(tags = "Sentinel Agent 接口")
@RequireRole({"ADMIN", "OPERATOR", "CUSTOMER_SERVICE"})
public class SentinelController {

    @Autowired
    private ContentGenAgent contentGenAgent;
    @Autowired
    private AnomalyDiagnoseAgent anomalyDiagnoseAgent;
    @Autowired
    private WorkorderAgent workorderAgent;
    @Autowired
    private CsRouteAgent csRouteAgent;
    @Autowired
    private AgentCallLogService agentCallLogService;
    @Autowired
    private CsSessionService csSessionService;
    @Autowired
    private SentinelFlowService sentinelFlowService;
    @Autowired
    private ObjectProvider<SentinelFlowRuleService> flowRuleServiceProvider;
    @Autowired
    private AuditLogService auditLogService;

    /**
     * Agent 1：文案生成
     */
    @PostMapping("/content/generate")
    @ApiOperation("Agent 1：文案生成（通知文案）")
    public BasicResultVO generateContent(
            @RequestParam String node,
            @RequestParam String language,
            @RequestParam(required = false, defaultValue = "商品信息") String productInfo,
            @RequestParam(required = false) String orderNo) {
        String traceId = agentCallLogService.generateTraceId();
        String content = contentGenAgent.generate(node, language, productInfo, orderNo, traceId);
        // 响应携带 traceId，调用方可通过 agent_call_log 溯源 status（success/degraded）
        JSONObject data = new JSONObject();
        data.put("content", content);
        data.put("traceId", traceId);
        return BasicResultVO.success(data);
    }

    /**
     * Agent 2：异常诊断
     */
    @PostMapping("/anomaly/diagnose")
    @ApiOperation("Agent 2：异常诊断")
    public BasicResultVO diagnoseAnomaly(
            @RequestParam String anomalyType,
            @RequestParam String statusCode,
            @RequestParam(required = false, defaultValue = "{}") String orderInfo) {
        String traceId = agentCallLogService.generateTraceId();
        JSONObject result = anomalyDiagnoseAgent.diagnose(anomalyType, statusCode, orderInfo, traceId);
        result.put("traceId", traceId);
        return BasicResultVO.success(result);
    }

    /**
     * Agent 5：工单处理
     */
    @PostMapping("/workorder/process")
    @ApiOperation("Agent 5：工单处理")
    public BasicResultVO processWorkorder(
            @RequestParam String anomalyDesc,
            @RequestParam(required = false, defaultValue = "{}") String orderInfo) {
        String traceId = agentCallLogService.generateTraceId();
        JSONObject result = workorderAgent.process(anomalyDesc, orderInfo, traceId);
        result.put("traceId", traceId);
        return BasicResultVO.success(result);
    }

    /**
     * Agent 6：客服路由
     */
    @PostMapping("/cs/chat")
    @ApiOperation("Agent 6：客服路由")
    public BasicResultVO csRoute(
            @RequestParam String message,
            @RequestParam(required = false) String buyerId) {
        String traceId = agentCallLogService.generateTraceId();
        JSONObject result = csRouteAgent.route(message, buyerId, traceId);
        result.put("traceId", traceId);
        csSessionService.record(buyerId, message, result);
        return BasicResultVO.success(result);
    }

    /**
     * 客服 Chat 最近一次结果（前端页面使用）
     */
    @GetMapping("/cs/lastResult")
    @ApiOperation("客服路由最近结果")
    public BasicResultVO csLastResult() {
        return BasicResultVO.success(csSessionService.lastResult());
    }

    /**
     * 客服 Chat 历史（前端页面使用）
     */
    @GetMapping("/cs/history")
    @ApiOperation("客服路由历史")
    public BasicResultVO csHistory() {
        return BasicResultVO.success(csSessionService.history());
    }

    /**
     * LiteFlow 异常处置编排入口。
     */
    @PostMapping("/flow/exception")
    @ApiOperation("异常处置编排流程")
    public BasicResultVO executeExceptionFlow(
            @RequestParam String orderNo,
            @RequestParam String anomalyDesc,
            @RequestParam(required = false) String node,
            @RequestParam(required = false, defaultValue = "false") boolean lowConfidence,
            @RequestParam(required = false) String traceId) {
        if (traceId == null || traceId.trim().isEmpty()) {
            traceId = agentCallLogService.generateTraceId();
        }
        SentinelFlowContext context = new SentinelFlowContext();
        context.setOrderNo(orderNo);
        context.setAnomalyDesc(anomalyDesc);
        context.setLogisticsNode(node);
        context.setLowConfidence(lowConfidence);
        context.setTraceId(traceId);
        return BasicResultVO.success(sentinelFlowService.execute(context));
    }

    /**
     * 查询当前 DB 规则。
     */
    @GetMapping("/flow/rule")
    @RequireRole({"ADMIN"})
    @ApiOperation("查询异常处置规则")
    public BasicResultVO flowRule() {
        SentinelFlowRuleService ruleService = flowRuleServiceProvider.getIfAvailable();
        if (ruleService == null) {
            return BasicResultVO.fail("Rule-DB 未启用");
        }
        return BasicResultVO.success(ruleService.currentRule());
    }

    /**
     * 发布规则并触发热更新；expectedVersion 可用于乐观锁。
     */
    @PostMapping("/flow/rule")
    @RequireRole({"ADMIN"})
    @ApiOperation("发布异常处置规则")
    public BasicResultVO publishFlowRule(
            @RequestParam String el,
            @RequestParam(required = false) Long expectedVersion) {
        SentinelFlowRuleService ruleService = flowRuleServiceProvider.getIfAvailable();
        if (ruleService == null) {
            return BasicResultVO.fail("Rule-DB 未启用");
        }
        java.util.Map<String, Object> result = ruleService.publish(el, expectedVersion);
        auditLogService.log("flow", "RULE_PUBLISH", SentinelFlowConfig.CHAIN_ID,
                "版本=" + result.get("version") + " expectedVersion=" + expectedVersion);
        return BasicResultVO.success(result);
    }
}
