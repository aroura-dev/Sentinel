package com.aroura.sentinel.web.controller.sentinel.tms;

import com.aroura.sentinel.common.vo.BasicResultVO;
import com.aroura.sentinel.web.annotation.RequireRole;
import com.aroura.sentinel.web.config.AuthInterceptor;
import com.aroura.sentinel.web.exception.CommonException;
import com.aroura.sentinel.web.service.sentinel.tms.BillingService;
import com.aroura.sentinel.web.service.sentinel.tms.MerchantService;
import com.aroura.sentinel.web.vo.CurrentUserVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * 计费结算接口（FINANCE/ADMIN；MERCHANT 仅见己方）
 *
 * @author sentinel
 */
@RestController
@RequestMapping("/api/tms/bill")
@Api(tags = "TMS 计费结算")
public class BillController {

    private final BillingService billingService;
    private final MerchantService merchantService;

    public BillController(BillingService billingService, MerchantService merchantService) {
        this.billingService = billingService;
        this.merchantService = merchantService;
    }

    @PostMapping("/generate")
    @ApiOperation("生成账单（承运商×账期）")
    @RequireRole({"FINANCE", "ADMIN"})
    public BasicResultVO generate(@RequestParam Long carrierId,
                                  @RequestParam String periodStart,
                                  @RequestParam String periodEnd) {
        return BasicResultVO.success(billingService.generate(carrierId, periodStart, periodEnd));
    }

    @PostMapping("/{id}/submit")
    @ApiOperation("提交账单")
    @RequireRole({"FINANCE", "ADMIN"})
    public BasicResultVO submit(@PathVariable Long id, HttpServletRequest request) {
        return BasicResultVO.success(billingService.transition(id, "submit", operator(request), null));
    }

    @PostMapping("/{id}/verify")
    @ApiOperation("核销账单")
    @RequireRole({"FINANCE", "ADMIN"})
    public BasicResultVO verify(@PathVariable Long id, HttpServletRequest request) {
        return BasicResultVO.success(billingService.transition(id, "verify", operator(request), null));
    }

    @PostMapping("/{id}/settle")
    @ApiOperation("结算账单")
    @RequireRole({"FINANCE", "ADMIN"})
    public BasicResultVO settle(@PathVariable Long id, HttpServletRequest request) {
        return BasicResultVO.success(billingService.transition(id, "settle", operator(request), null));
    }

    @PostMapping("/{id}/reject")
    @ApiOperation("驳回账单")
    @RequireRole({"FINANCE", "ADMIN"})
    public BasicResultVO reject(@PathVariable Long id,
                                @RequestParam(required = false) String reason,
                                HttpServletRequest request) {
        return BasicResultVO.success(billingService.transition(id, "reject", operator(request), reason));
    }

    @PostMapping("/{id}/reopen")
    @ApiOperation("重开账单（REJECTED→DRAFT）")
    @RequireRole({"FINANCE", "ADMIN"})
    public BasicResultVO reopen(@PathVariable Long id, HttpServletRequest request) {
        return BasicResultVO.success(billingService.transition(id, "reopen", operator(request), null));
    }

    @PostMapping("/{id}/add-waybill")
    @ApiOperation("手动入账：把未入账运单加入草稿账单")
    @RequireRole({"FINANCE", "ADMIN"})
    public BasicResultVO addWaybill(@PathVariable Long id, @RequestParam String waybillNo) {
        return BasicResultVO.success(billingService.addWaybill(id, waybillNo));
    }

    @GetMapping("/list")
    @ApiOperation("账单分页")
    @RequireRole({"FINANCE", "ADMIN"})
    public BasicResultVO list(@RequestParam(required = false) String status,
                              @RequestParam(required = false) Long carrierId,
                              @RequestParam(defaultValue = "1") Integer page,
                              @RequestParam(defaultValue = "10") Integer perPage) {
        return BasicResultVO.success(billingService.list(status, carrierId, page, perPage));
    }

    @GetMapping("/{id}")
    @ApiOperation("账单详情（含明细）")
    @RequireRole({"FINANCE", "ADMIN"})
    public BasicResultVO detail(@PathVariable Long id) {
        return BasicResultVO.success(billingService.detail(id));
    }

    @GetMapping("/stats")
    @ApiOperation("账单状态统计")
    @RequireRole({"FINANCE", "ADMIN"})
    public BasicResultVO stats() {
        return BasicResultVO.success(billingService.stats());
    }

    @GetMapping("/merchant/self")
    @ApiOperation("当前商家账单汇总")
    @RequireRole({"MERCHANT", "ADMIN"})
    public BasicResultVO merchantSelf(HttpServletRequest request) {
        Long merchantId = resolveMerchant(request);
        return BasicResultVO.success(billingService.merchantSelf(merchantId));
    }

    private Long resolveMerchant(HttpServletRequest request) {
        Object attr = request.getAttribute(AuthInterceptor.CURRENT_USER_ATTR);
        if (attr instanceof CurrentUserVO) {
            CurrentUserVO user = (CurrentUserVO) attr;
            Map<String, Object> merchant = merchantService.findByUsername(user.getUsername());
            if (merchant != null) {
                return Long.valueOf(String.valueOf(merchant.get("id")));
            }
        }
        throw new CommonException("未找到当前商家");
    }

    private String operator(HttpServletRequest request) {
        Object attr = request.getAttribute(AuthInterceptor.CURRENT_USER_ATTR);
        return attr instanceof CurrentUserVO ? ((CurrentUserVO) attr).getUsername() : "system";
    }
}
