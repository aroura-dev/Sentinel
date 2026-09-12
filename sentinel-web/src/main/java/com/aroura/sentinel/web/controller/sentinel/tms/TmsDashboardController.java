package com.aroura.sentinel.web.controller.sentinel.tms;

import com.aroura.sentinel.common.vo.BasicResultVO;
import com.aroura.sentinel.web.annotation.RequireRole;
import com.aroura.sentinel.web.config.AuthInterceptor;
import com.aroura.sentinel.web.exception.CommonException;
import com.aroura.sentinel.web.service.sentinel.tms.MerchantService;
import com.aroura.sentinel.web.service.sentinel.tms.TmsDashboardService;
import com.aroura.sentinel.web.vo.CurrentUserVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

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
    private final MerchantService merchantService;

    public TmsDashboardController(TmsDashboardService dashboardService, MerchantService merchantService) {
        this.dashboardService = dashboardService;
        this.merchantService = merchantService;
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
    public BasicResultVO seller(@RequestParam(required = false) Long merchantId, HttpServletRequest request) {
        Long merchant = resolveMerchant(merchantId, request);
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

    private Long resolveMerchant(Long merchantId, HttpServletRequest request) {
        Object attr = request.getAttribute(AuthInterceptor.CURRENT_USER_ATTR);
        if (attr instanceof CurrentUserVO && "MERCHANT".equals(((CurrentUserVO) attr).getRole())) {
            Map<String, Object> m = merchantService.findByUsername(((CurrentUserVO) attr).getUsername());
            if (m == null) {
                throw new CommonException("未找到当前商家");
            }
            return Long.valueOf(String.valueOf(m.get("id")));
        }
        // ADMIN/OPERATOR：merchantId 可空 → 聚合全部商家
        return merchantId;
    }
}
