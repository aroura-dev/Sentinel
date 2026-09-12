package com.java3y.austin.web.controller.sentinel.tms;

import com.java3y.austin.common.vo.BasicResultVO;
import com.java3y.austin.web.annotation.RequireRole;
import com.java3y.austin.web.service.sentinel.tms.SlaAnalysisService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 时效分析接口（运营 → 时效分析）
 *
 * @author sentinel
 */
@RestController
@RequestMapping("/api/tms/sla")
@Api(tags = "时效分析")
public class SlaAnalysisController {

    private final SlaAnalysisService slaService;

    public SlaAnalysisController(SlaAnalysisService slaService) {
        this.slaService = slaService;
    }

    @GetMapping("/overview")
    @ApiOperation("时效总览")
    @RequireRole({"ADMIN", "OPERATOR", "FINANCE"})
    public BasicResultVO overview(@RequestParam(required = false) String start,
                                       @RequestParam(required = false) String end) {
        return BasicResultVO.success(slaService.overview(start, end));
    }

    @GetMapping("/by-channel")
    @ApiOperation("渠道准时率")
    @RequireRole({"ADMIN", "OPERATOR", "FINANCE"})
    public BasicResultVO byChannel(@RequestParam(required = false) String start,
                                        @RequestParam(required = false) String end) {
        return BasicResultVO.success(slaService.byChannel(start, end));
    }

    @GetMapping("/by-carrier")
    @ApiOperation("承运商时效")
    @RequireRole({"ADMIN", "OPERATOR", "FINANCE"})
    public BasicResultVO byCarrier(@RequestParam(required = false) String start,
                                        @RequestParam(required = false) String end) {
        return BasicResultVO.success(slaService.byCarrier(start, end));
    }

    @GetMapping("/top-delay")
    @ApiOperation("延误 Top 原因")
    @RequireRole({"ADMIN", "OPERATOR", "FINANCE"})
    public BasicResultVO topDelay(@RequestParam(required = false) String start,
                                       @RequestParam(required = false) String end) {
        return BasicResultVO.success(slaService.topDelay(start, end));
    }
}
