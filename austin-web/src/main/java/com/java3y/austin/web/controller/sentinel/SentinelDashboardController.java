package com.java3y.austin.web.controller.sentinel;

import com.java3y.austin.common.vo.BasicResultVO;
import com.java3y.austin.logistics.dao.DashboardDao;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 看板统计接口（图表数据基于真实业务表）
 *
 * @author sentinel
 */
@RestController
@RequestMapping("/api/dashboard")
@Api(tags = "Sentinel 看板统计接口")
public class SentinelDashboardController {

    @Autowired
    private DashboardDao dashboardDao;

    @GetMapping("/channel-distribution")
    @ApiOperation("近 7 日通知渠道分布")
    public BasicResultVO channelDistribution() {
        return BasicResultVO.success(dashboardDao.channelDistribution());
    }

    @GetMapping("/agent-trend")
    @ApiOperation("近 7 日 Agent 调用趋势")
    public BasicResultVO agentTrend() {
        return BasicResultVO.success(dashboardDao.agentTrend());
    }
}