package com.aroura.sentinel.web.controller.sentinel.tms;

import com.aroura.sentinel.common.vo.BasicResultVO;
import com.aroura.sentinel.web.annotation.RequireRole;
import com.aroura.sentinel.web.service.sentinel.tms.ReconcileService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 差异对账接口（结算 → 差异对账）
 *
 * @author sentinel
 */
@RestController
@RequestMapping("/api/tms/reconcile")
@Api(tags = "差异对账")
public class ReconcileController {

    private final ReconcileService reconcileService;

    public ReconcileController(ReconcileService reconcileService) {
        this.reconcileService = reconcileService;
    }

    @GetMapping("/bills")
    @ApiOperation("可选账单列表")
    @RequireRole({"ADMIN", "FINANCE"})
    public BasicResultVO bills() {
        return BasicResultVO.success(reconcileService.bills());
    }

    @GetMapping("/{billId}")
    @ApiOperation("对账明细（系统 vs 承运商口径）")
    @RequireRole({"ADMIN", "FINANCE"})
    public BasicResultVO detail(@PathVariable Long billId) {
        return BasicResultVO.success(reconcileService.reconcile(billId));
    }

    @PostMapping("/{billId}/mark")
    @ApiOperation("标记差异原因（写操作审计）")
    @RequireRole({"ADMIN", "FINANCE"})
    public BasicResultVO mark(@PathVariable Long billId,
                              @RequestParam String waybillNo,
                              @RequestParam String reason) {
        return BasicResultVO.success(reconcileService.mark(billId, waybillNo, reason));
    }
}
