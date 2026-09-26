package com.aroura.sentinel.web.controller.sentinel.tms;

import com.aroura.sentinel.common.vo.BasicResultVO;
import com.aroura.sentinel.logistics.dao.LogisticsDao;
import com.aroura.sentinel.web.annotation.RequireRole;
import com.aroura.sentinel.web.exception.CommonException;
import com.aroura.sentinel.web.service.sentinel.tms.FulfillmentService;
import com.aroura.sentinel.web.service.sentinel.tms.TmsOperationService;
import com.aroura.sentinel.web.service.sentinel.tms.WaybillService;
import com.aroura.sentinel.web.support.TenantScopeResolver;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 履约闭环接口：运费试算 / 建单 / 运单
 *
 * @author sentinel
 */
@RestController
@RequestMapping("/api/tms")
@Api(tags = "TMS 履约闭环")
public class FulfillmentController {

    private final FulfillmentService fulfillmentService;
    private final WaybillService waybillService;
    private final TmsOperationService operationService;
    private final LogisticsDao logisticsDao;
    private final TenantScopeResolver tenantScope;

    public FulfillmentController(FulfillmentService fulfillmentService, WaybillService waybillService,
                                 TmsOperationService operationService,
                                 LogisticsDao logisticsDao, TenantScopeResolver tenantScope) {
        this.fulfillmentService = fulfillmentService;
        this.waybillService = waybillService;
        this.operationService = operationService;
        this.logisticsDao = logisticsDao;
        this.tenantScope = tenantScope;
    }

    @PostMapping("/quote")
    @ApiOperation("运费试算")
    @RequireRole({"ADMIN", "OPERATOR", "FINANCE", "MERCHANT"})
    public BasicResultVO quote(@RequestBody Map<String, Object> body) {
        applyMerchantScope(body);
        return BasicResultVO.success(fulfillmentService.quote(body));
    }

    @PostMapping("/quote/compare")
    @ApiOperation("渠道比价（目的地全部渠道批量报价，按运费升序）")
    @RequireRole({"ADMIN", "OPERATOR", "FINANCE", "MERCHANT"})
    public BasicResultVO compareChannels(@RequestBody Map<String, Object> body) {
        applyMerchantScope(body);
        return BasicResultVO.success(fulfillmentService.compareChannels(body));
    }

    @PostMapping("/order/create")
    @ApiOperation("创建履约订单")
    @RequireRole({"ADMIN", "OPERATOR", "MERCHANT"})
    public BasicResultVO createOrder(@RequestBody Map<String, Object> body) {
        applyMerchantScope(body);
        return BasicResultVO.success(fulfillmentService.createOrder(body));
    }

    @GetMapping("/order/list")
    @ApiOperation("履约订单分页")
    @RequireRole({"ADMIN", "OPERATOR", "FINANCE", "MERCHANT"})
    public BasicResultVO orderList(@RequestParam(required = false) Long merchantId,
                                   @RequestParam(required = false) Long channelId,
                                   @RequestParam(required = false) String slaStatus,
                                   @RequestParam(required = false) String node,
                                   @RequestParam(required = false) String startDate,
                                   @RequestParam(required = false) String endDate,
                                   @RequestParam(required = false) String keyword,
                                   @RequestParam(defaultValue = "1") Integer page,
                                   @RequestParam(defaultValue = "10") Integer perPage) {
        // MERCHANT 的入参一律被自身作用域替换（静默，不报错）：
        // 前端会把商户筛选持久化到 localStorage，报错会让商家登录后直接不可用
        Long scope = tenantScope.normalizeRequested(merchantId);
        return BasicResultVO.success(logisticsDao.findPageTms(scope, channelId, slaStatus, node,
                startDate, endDate, keyword, page, perPage));
    }

    @GetMapping("/order/{orderNo}")
    @ApiOperation("订单详情")
    @RequireRole({"ADMIN", "OPERATOR", "FINANCE", "MERCHANT"})
    public BasicResultVO orderDetail(@PathVariable String orderNo) {
        Map<String, Object> order = logisticsDao.findOrderByNo(orderNo);
        tenantScope.assertAccessible(merchantIdOf(order), "订单");
        return BasicResultVO.success(order);
    }

    @GetMapping("/order/{orderNo}/tracks")
    @ApiOperation("订单轨迹")
    @RequireRole({"ADMIN", "OPERATOR", "FINANCE", "MERCHANT"})
    public BasicResultVO orderTracks(@PathVariable String orderNo) {
        // logistics_track 表本身没有 merchant_id，须先经由订单判定归属
        Map<String, Object> order = logisticsDao.findOrderByNo(orderNo);
        tenantScope.assertAccessible(merchantIdOf(order), "订单");
        return BasicResultVO.success(logisticsDao.listTracks(orderNo));
    }

    @PostMapping("/order/{orderNo}/advance")
    @ApiOperation("手动推进物流节点")
    @RequireRole({"ADMIN", "OPERATOR", "CUSTOMER_SERVICE"})
    public BasicResultVO advance(@PathVariable String orderNo) {
        return BasicResultVO.success(operationService.advance(orderNo));
    }

    @PostMapping("/order/{orderNo}/anomaly")
    @ApiOperation("模拟异常（customs_delay/delivery_failed/lost/returned）")
    @RequireRole({"ADMIN", "OPERATOR"})
    public BasicResultVO injectAnomaly(@PathVariable String orderNo, @RequestParam String type) {
        return BasicResultVO.success(operationService.injectAnomaly(orderNo, type));
    }

    @PutMapping("/order/{orderNo}")
    @ApiOperation("编辑订单（买家/地址/语言/业务备注；已出库订单收货信息锁定）")
    @RequireRole({"ADMIN", "OPERATOR", "MERCHANT"})
    public BasicResultVO updateOrder(@PathVariable String orderNo, @RequestBody Map<String, Object> body) {
        // 归属断言落在 TmsOperationService.updateOrder 内部（取回订单之后），此处不再重复判
        return BasicResultVO.success(operationService.updateOrder(orderNo, body));
    }

    @PostMapping("/order/{orderNo}/cancel")
    @ApiOperation("取消订单（仅未出库可直接取消；已出库走售后退回）")
    @RequireRole({"ADMIN", "OPERATOR", "MERCHANT"})
    public BasicResultVO cancelOrder(@PathVariable String orderNo) {
        return BasicResultVO.success(operationService.cancelOrder(orderNo));
    }

    @PostMapping("/waybill/generate")
    @ApiOperation("出库生成运单（整单/分批出库；分批传 items 子集 + partial=true）")
    @RequireRole({"ADMIN", "OPERATOR"})
    public BasicResultVO generateWaybill(@RequestParam String orderNo,
                                         @RequestParam(required = false) String items,
                                         @RequestParam(defaultValue = "false") boolean partial) {
        return BasicResultVO.success(waybillService.generate(orderNo, items, partial));
    }

    @PostMapping("/waybill/merge")
    @ApiOperation("合并运单（同商家同目的地未出库已审核订单合并生成一张运单）")
    @RequireRole({"ADMIN", "OPERATOR"})
    public BasicResultVO mergeWaybills(@RequestBody Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        java.util.List<String> orderNos = (java.util.List<String>) body.get("orderNos");
        return BasicResultVO.success(waybillService.merge(orderNos));
    }

    @GetMapping("/waybill/list")
    @ApiOperation("运单分页")
    @RequireRole({"ADMIN", "OPERATOR", "FINANCE", "MERCHANT"})
    public BasicResultVO waybillList(@RequestParam(required = false) String orderNo,
                                     @RequestParam(required = false) String waybillNo,
                                     @RequestParam(required = false) String trackingNo,
                                     @RequestParam(required = false) Long channelId,
                                     @RequestParam(required = false) Long carrierId,
                                     @RequestParam(defaultValue = "1") Integer page,
                                     @RequestParam(defaultValue = "10") Integer perPage) {
        return BasicResultVO.success(waybillService.list(orderNo, waybillNo, trackingNo, channelId,
                carrierId, tenantScope.currentScope(), page, perPage));
    }

    @GetMapping("/waybill/{waybillNo}")
    @ApiOperation("运单详情")
    @RequireRole({"ADMIN", "OPERATOR", "FINANCE", "MERCHANT"})
    public BasicResultVO waybillDetail(@PathVariable String waybillNo) {
        // 归属断言落在 WaybillService.detail 内部，三个调用方一并覆盖
        return BasicResultVO.success(waybillService.detail(waybillNo));
    }

    @GetMapping("/waybill/{waybillNo}/tracks")
    @ApiOperation("运单轨迹（按运单号查订单轨迹）")
    @RequireRole({"ADMIN", "OPERATOR", "FINANCE", "MERCHANT"})
    public BasicResultVO waybillTracks(@PathVariable String waybillNo) {
        Map<String, Object> wb = waybillService.detail(waybillNo);
        if (wb == null) {
            throw new CommonException("运单不存在：" + waybillNo);
        }
        tenantScope.assertAccessible(merchantIdOf(wb), "运单");
        return BasicResultVO.success(logisticsDao.listTracks(String.valueOf(wb.get("order_no"))));
    }

    /* ---------- MERCHANT 权限隔离 ---------- */

    /**
     * 建单/报价类请求体的商家归属：MERCHANT 一律替换为自身商家（静默，不报错），
     * 平台角色保留请求体原值（代操作）。MERCHANT 未绑定商家档案时 {@code currentScope()} 会直接拒绝。
     */
    private void applyMerchantScope(Map<String, Object> body) {
        Long scope = tenantScope.currentScope();
        if (scope != null) {
            body.put("merchantId", scope);
        }
    }

    private static Long merchantIdOf(Map<String, Object> row) {
        if (row == null || row.get("merchant_id") == null) {
            return null;
        }
        return Long.valueOf(String.valueOf(row.get("merchant_id")));
    }
}
