package com.aroura.sentinel.web.controller.sentinel.tms;

import com.aroura.sentinel.common.vo.BasicResultVO;
import com.aroura.sentinel.web.annotation.RequireRole;
import com.aroura.sentinel.web.service.sentinel.tms.ClaimService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 理赔流程接口（薄控制器，业务在 {@link ClaimService}）
 *
 * @author sentinel
 */
@RestController
@RequestMapping("/api/tms/claim")
@Api(tags = "理赔流程")
public class ClaimController {

    private final ClaimService claimService;

    public ClaimController(ClaimService claimService) {
        this.claimService = claimService;
    }

    @GetMapping("/list")
    @ApiOperation("理赔单分页")
    @RequireRole({"ADMIN", "OPERATOR", "CUSTOMER_SERVICE", "FINANCE"})
    public BasicResultVO list(@RequestParam(required = false) String claimStatus,
                              @RequestParam(required = false) String orderNo,
                              @RequestParam(defaultValue = "1") int page,
                              @RequestParam(defaultValue = "10") int perPage) {
        return BasicResultVO.success(claimService.list(claimStatus, orderNo, page, perPage));
    }

    @GetMapping("/stats")
    @ApiOperation("理赔状态统计")
    @RequireRole({"ADMIN", "OPERATOR", "CUSTOMER_SERVICE", "FINANCE"})
    public BasicResultVO stats() {
        return BasicResultVO.success(claimService.stats());
    }

    @GetMapping("/{id}")
    @ApiOperation("理赔单详情")
    @RequireRole({"ADMIN", "OPERATOR", "CUSTOMER_SERVICE", "FINANCE"})
    public BasicResultVO detail(@PathVariable Long id) {
        return BasicResultVO.success(claimService.detail(id));
    }

    @PostMapping("/register")
    @ApiOperation("登记理赔（进入待审批）")
    @RequireRole({"ADMIN", "OPERATOR", "CUSTOMER_SERVICE"})
    public BasicResultVO register(@RequestBody Map<String, Object> body) {
        return BasicResultVO.success(claimService.register(
                body.get("orderNo") == null ? null : String.valueOf(body.get("orderNo")),
                body.get("type") == null ? null : String.valueOf(body.get("type")),
                body.get("description") == null ? null : String.valueOf(body.get("description")),
                body.get("liability") == null ? null : String.valueOf(body.get("liability")),
                body.get("claimAmount") == null ? null : new BigDecimal(String.valueOf(body.get("claimAmount")))));
    }

    @PostMapping("/{id}/approve")
    @ApiOperation("审批通过（核定赔付金额，进入待赔付）")
    @RequireRole({"ADMIN", "OPERATOR", "CUSTOMER_SERVICE"})
    public BasicResultVO approve(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        return BasicResultVO.success(claimService.approve(id,
                body.get("compensationAmount") == null ? null : new BigDecimal(String.valueOf(body.get("compensationAmount")))));
    }

    @PostMapping("/{id}/pay")
    @ApiOperation("确认赔付（财务执行）")
    @RequireRole({"ADMIN", "FINANCE"})
    public BasicResultVO pay(@PathVariable Long id) {
        return BasicResultVO.success(claimService.pay(id));
    }

    @PostMapping("/{id}/reject")
    @ApiOperation("驳回理赔")
    @RequireRole({"ADMIN", "OPERATOR", "CUSTOMER_SERVICE"})
    public BasicResultVO reject(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        return BasicResultVO.success(claimService.reject(id,
                body.get("reason") == null ? null : String.valueOf(body.get("reason"))));
    }
}
