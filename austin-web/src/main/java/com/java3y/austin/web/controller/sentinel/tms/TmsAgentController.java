package com.java3y.austin.web.controller.sentinel.tms;

import com.java3y.austin.agent.agent.EtaPredictAgent;
import com.java3y.austin.agent.agent.RouteAdviceAgent;
import com.java3y.austin.agent.service.AgentCallLogService;
import com.java3y.austin.common.vo.BasicResultVO;
import com.java3y.austin.web.annotation.RequireRole;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * TMS AI 智能能力直调接口（渠道推荐 / 时效预测）
 *
 * @author sentinel
 */
@RestController
@RequestMapping("/api/tms/agent")
@Api(tags = "TMS AI 智能能力")
public class TmsAgentController {

    private final RouteAdviceAgent routeAdviceAgent;
    private final EtaPredictAgent etaPredictAgent;
    private final AgentCallLogService agentCallLogService;

    public TmsAgentController(RouteAdviceAgent routeAdviceAgent, EtaPredictAgent etaPredictAgent,
                              AgentCallLogService agentCallLogService) {
        this.routeAdviceAgent = routeAdviceAgent;
        this.etaPredictAgent = etaPredictAgent;
        this.agentCallLogService = agentCallLogService;
    }

    @PostMapping("/route-advice")
    @ApiOperation("智能渠道推荐（真实价卡 + LLM 权衡）")
    @RequireRole({"ADMIN", "OPERATOR", "MERCHANT"})
    public BasicResultVO routeAdvice(@RequestParam String destCountry, @RequestParam String weightKg) {
        String traceId = agentCallLogService.generateTraceId();
        return BasicResultVO.success(routeAdviceAgent.advise(destCountry, weightKg, traceId));
    }

    @PostMapping("/eta-predict")
    @ApiOperation("时效预测（承诺时效 + 当前进度）")
    @RequireRole({"ADMIN", "OPERATOR", "CUSTOMER_SERVICE", "MERCHANT"})
    public BasicResultVO etaPredict(@RequestParam String orderNo) {
        String traceId = agentCallLogService.generateTraceId();
        return BasicResultVO.success(etaPredictAgent.predict(orderNo, traceId));
    }
}
