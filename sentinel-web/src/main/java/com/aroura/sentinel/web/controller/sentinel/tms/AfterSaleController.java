package com.aroura.sentinel.web.controller.sentinel.tms;

import com.aroura.sentinel.common.vo.BasicResultVO;
import com.aroura.sentinel.web.annotation.RequireRole;
import com.aroura.sentinel.web.service.sentinel.tms.AfterSaleService;
import com.aroura.sentinel.web.support.TenantScopeResolver;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 逆向售后退货闭环接口
 *
 * @author sentinel
 */
@RestController
@RequestMapping("/api/tms/after-sale")
@Api(tags = "逆向售后")
public class AfterSaleController {

    private final AfterSaleService afterSaleService;
    private final TenantScopeResolver tenantScope;

    public AfterSaleController(AfterSaleService afterSaleService, TenantScopeResolver tenantScope) {
        this.afterSaleService = afterSaleService;
        this.tenantScope = tenantScope;
    }

    @GetMapping("/list")
    @ApiOperation("售后单列表")
    @RequireRole({"ADMIN", "OPERATOR", "CUSTOMER_SERVICE", "MERCHANT"})
    public BasicResultVO list(@RequestParam(required = false) String orderNo,
                              @RequestParam(required = false) String status,
                              @RequestParam(defaultValue = "1") int page,
                              @RequestParam(defaultValue = "10") int perPage) {
        return BasicResultVO.success(afterSaleService.list(orderNo, status,
                tenantScope.currentScope(), page, perPage));
    }

    @GetMapping("/{id}")
    @ApiOperation("售后单详情")
    @RequireRole({"ADMIN", "OPERATOR", "CUSTOMER_SERVICE", "MERCHANT"})
    public BasicResultVO detail(@PathVariable Long id) {
        return BasicResultVO.success(afterSaleService.detail(id));
    }

    @PostMapping("/register")
    @ApiOperation("登记退货")
    @RequireRole({"ADMIN", "OPERATOR", "CUSTOMER_SERVICE"})
    public BasicResultVO register(@RequestBody Map<String, Object> body) {
        return BasicResultVO.success(afterSaleService.register(
                String.valueOf(body.get("orderNo")),
                body.get("reason") == null ? null : String.valueOf(body.get("reason")),
                body.get("type") == null ? null : String.valueOf(body.get("type"))));
    }

    @PostMapping("/{id}/refund")
    @ApiOperation("受理并退款")
    @RequireRole({"ADMIN", "OPERATOR", "FINANCE"})
    public BasicResultVO refund(@PathVariable Long id) {
        return BasicResultVO.success(afterSaleService.refund(id));
    }

    @PostMapping("/{id}/reship")
    @ApiOperation("换货重发")
    @RequireRole({"ADMIN", "OPERATOR"})
    public BasicResultVO reship(@PathVariable Long id) {
        return BasicResultVO.success(afterSaleService.reship(id));
    }

    @PostMapping("/{id}/close")
    @ApiOperation("关闭售后")
    @RequireRole({"ADMIN", "OPERATOR", "CUSTOMER_SERVICE"})
    public BasicResultVO close(@PathVariable Long id) {
        return BasicResultVO.success(afterSaleService.close(id));
    }
}
