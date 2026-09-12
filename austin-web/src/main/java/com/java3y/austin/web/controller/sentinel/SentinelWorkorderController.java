package com.java3y.austin.web.controller.sentinel;

import com.java3y.austin.common.vo.BasicResultVO;
import com.java3y.austin.web.annotation.RequireRole;
import com.java3y.austin.web.config.AuthInterceptor;
import com.java3y.austin.web.exception.CommonException;
import com.java3y.austin.web.service.sentinel.WorkorderService;
import com.java3y.austin.web.service.sentinel.tms.MerchantService;
import com.java3y.austin.web.vo.CurrentUserVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.GetMapping;

import java.math.BigDecimal;
import javax.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 异常工单接口（薄控制器，业务逻辑在 {@link WorkorderService}）
 *
 * @author sentinel
 */
@RestController
@RequestMapping("/api/workorder")
@Api(tags = "Sentinel 工单接口")
public class SentinelWorkorderController {

    private final WorkorderService workorderService;
    private final MerchantService merchantService;

    public SentinelWorkorderController(WorkorderService workorderService, MerchantService merchantService) {
        this.workorderService = workorderService;
        this.merchantService = merchantService;
    }

    @GetMapping("/stats")
    @ApiOperation("工单统计")
    public BasicResultVO stats() {
        return BasicResultVO.success(workorderService.stats());
    }

    @GetMapping("/list")
    @ApiOperation("工单分页")
    public BasicResultVO list(@RequestParam(required = false) String status,
                              @RequestParam(required = false) String level,
                              @RequestParam(required = false) String orderNo,
                              @RequestParam(defaultValue = "1") Integer page,
                              @RequestParam(defaultValue = "10") Integer perPage,
                              HttpServletRequest request) {
        Long merchantId = null;
        Object attr = request.getAttribute(AuthInterceptor.CURRENT_USER_ATTR);
        if (attr instanceof CurrentUserVO && "MERCHANT".equals(((CurrentUserVO) attr).getRole())) {
            // 商家数据隔离：只能看自己订单的工单
            Map<String, Object> m = merchantService.findByUsername(((CurrentUserVO) attr).getUsername());
            if (m == null) {
                throw new CommonException("未找到当前商家");
            }
            merchantId = Long.valueOf(String.valueOf(m.get("id")));
        }
        return BasicResultVO.success(workorderService.list(status, level, orderNo, merchantId, page, perPage));
    }

    @GetMapping("/{id}")
    @ApiOperation("工单详情")
    public BasicResultVO detail(@PathVariable Long id) {
        Object row = workorderService.detail(id);
        return row == null ? BasicResultVO.fail("工单不存在") : BasicResultVO.success(row);
    }

    @PostMapping("/push")
    @ApiOperation("推送工单到 OmniMerchant")
    @RequireRole({"ADMIN", "CUSTOMER_SERVICE", "OPERATOR"})
    public BasicResultVO push(@RequestParam Long workorderId) {
        return workorderService.push(workorderId)
                ? BasicResultVO.success(true)
                : BasicResultVO.fail("工单不存在");
    }

    @PostMapping("/process")
    @ApiOperation("Agent 处理异常并创建工单")
    @RequireRole({"ADMIN", "CUSTOMER_SERVICE", "OPERATOR"})
    public BasicResultVO process(@RequestParam String orderNo,
                                 @RequestParam(required = false, defaultValue = "物流异常待处理") String anomalyDesc) {
        return BasicResultVO.success(workorderService.process(orderNo, anomalyDesc));
    }

    @PostMapping("/{id}/diagnose")
    @ApiOperation("AI 诊断工单（结合异常知识库，返回原因/建议/优先级）")
    @RequireRole({"ADMIN", "CUSTOMER_SERVICE", "OPERATOR"})
    public BasicResultVO diagnose(@PathVariable Long id) {
        Object result = workorderService.diagnose(id);
        return result == null ? BasicResultVO.fail("工单不存在") : BasicResultVO.success(result);
    }

    @PostMapping("/{id}/status")
    @ApiOperation("更新工单状态（OPEN/PROCESSING/RESOLVED/CLOSED/PUSHED）")
    @RequireRole({"ADMIN", "CUSTOMER_SERVICE", "OPERATOR"})
    public BasicResultVO updateStatus(@PathVariable Long id, @RequestParam String status) {
        try {
            return workorderService.updateStatus(id, status)
                    ? BasicResultVO.success(true)
                    : BasicResultVO.fail("工单不存在");
        } catch (IllegalArgumentException | IllegalStateException | SecurityException e) {
            return BasicResultVO.fail(e.getMessage());
        }
    }

    @PostMapping("/{id}/claim")
    @ApiOperation("登记索赔（责任方/索赔额/理赔额，工单置 RESOLVED）")
    @RequireRole({"ADMIN", "CUSTOMER_SERVICE", "OPERATOR", "FINANCE"})
    public BasicResultVO claim(@PathVariable Long id,
                               @RequestParam String liability,
                               @RequestParam(required = false, defaultValue = "0") BigDecimal claimAmount,
                               @RequestParam(required = false, defaultValue = "0") BigDecimal compensationAmount,
                               @RequestParam(required = false) String resolution) {
        return workorderService.claim(id, liability, claimAmount, compensationAmount, resolution)
                ? BasicResultVO.success(true)
                : BasicResultVO.fail("工单不存在");
    }
}
