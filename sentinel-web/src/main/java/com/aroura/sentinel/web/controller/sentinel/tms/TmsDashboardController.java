package com.aroura.sentinel.web.controller.sentinel.tms;

import com.aroura.sentinel.common.vo.BasicResultVO;
import com.aroura.sentinel.web.annotation.RequireRole;
import com.aroura.sentinel.web.service.sentinel.tms.TmsDashboardService;
import com.aroura.sentinel.web.support.TenantScopeResolver;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * TMS 看板接口
 *
 * @author sentinel
 */
@RestController
@RequestMapping("/api/tms/dashboard")
@Api(tags = "TMS 看板")
public class TmsDashboardController {

    private final TmsDashboardService dashboardService;
    private final TenantScopeResolver tenantScope;

    public TmsDashboardController(TmsDashboardService dashboardService, TenantScopeResolver tenantScope) {
        this.dashboardService = dashboardService;
        this.tenantScope = tenantScope;
    }

    @GetMapping("/overview")
    @ApiOperation("平台总览")
    @RequireRole({"ADMIN", "OPERATOR", "FINANCE"})
    public BasicResultVO overview() {
        return BasicResultVO.success(dashboardService.overview());
    }

    @GetMapping("/seller")
    @ApiOperation("卖家看板")
    @RequireRole({"ADMIN", "OPERATOR", "MERCHANT"})
    public BasicResultVO seller(@RequestParam(required = false) Long merchantId) {
        Long merchant = resolveMerchant(merchantId);
        return BasicResultVO.success(dashboardService.seller(merchant));
    }

    @GetMapping("/order-trend")
    @ApiOperation("近7天订单趋势")
    @RequireRole({"ADMIN", "OPERATOR", "FINANCE"})
    public BasicResultVO orderTrend() {
        return BasicResultVO.success(dashboardService.orderTrend());
    }

    @GetMapping("/carrier-volume")
    @ApiOperation("承运商业务量")
    @RequireRole({"ADMIN", "OPERATOR", "FINANCE"})
    public BasicResultVO carrierVolume() {
        return BasicResultVO.success(dashboardService.carrierVolume());
    }

    @GetMapping("/channel-mix")
    @ApiOperation("渠道运量分布")
    @RequireRole({"ADMIN", "OPERATOR", "FINANCE"})
    public BasicResultVO channelMix() {
        return BasicResultVO.success(dashboardService.channelMix());
    }

    @GetMapping("/anomaly-rate")
    @ApiOperation("异常分布")
    @RequireRole({"ADMIN", "OPERATOR", "CUSTOMER_SERVICE"})
    public BasicResultVO anomalyRate() {
        return BasicResultVO.success(dashboardService.anomalyRate());
    }

    @GetMapping("/settlement")
    @ApiOperation("结算分布")
    @RequireRole({"ADMIN", "OPERATOR", "FINANCE"})
    public BasicResultVO settlement() {
        return BasicResultVO.success(dashboardService.settlement());
    }

    /**
     * MERCHANT → 自身商家（入参静默忽略）；ADMIN/OPERATOR → 原样透传，可空（空 = 聚合全部商家）。
     * <p>
     * 注意这里必须走 {@code normalizeRequested} 而不是 {@code currentScope()}：
     * 后者对平台角色恒返回 null，会让"管理员切换商家看板"整块退化。
     */
    private Long resolveMerchant(Long merchantId) {
        return tenantScope.normalizeRequested(merchantId);
    }
}
