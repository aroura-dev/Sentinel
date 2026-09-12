package com.aroura.sentinel.web.controller;

import com.aroura.sentinel.common.vo.BasicResultVO;
import com.aroura.sentinel.logistics.model.LogisticsTrack;
import com.aroura.sentinel.web.annotation.RequireRole;
import com.aroura.sentinel.web.service.sentinel.LogisticsService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.Map;

/**
 * Sentinel 物流接口（薄控制器，业务逻辑在 {@link LogisticsService}）
 *
 * @author sentinel
 */
@RestController
@RequestMapping("/api/logistics")
@Api(tags = "Sentinel 物流接口")
public class LogisticsController {

    private final LogisticsService logisticsService;

    public LogisticsController(LogisticsService logisticsService) {
        this.logisticsService = logisticsService;
    }

    @PostMapping("/mock/create")
    @ApiOperation("Mock 创建物流订单")
    @RequireRole({"ADMIN", "OPERATOR"})
    public BasicResultVO createOrder(
            @RequestParam(required = false, defaultValue = "buyer001") String buyerId,
            @RequestParam(required = false, defaultValue = "zh") String language,
            @RequestParam(required = false, defaultValue = "广东") String country) {
        return BasicResultVO.success(logisticsService.createOrder(buyerId, language, country));
    }

    @GetMapping("/order/{orderNo}")
    @ApiOperation("查询物流订单")
    public BasicResultVO getOrder(@PathVariable String orderNo) {
        Map<String, Object> row = logisticsService.getOrder(orderNo);
        return row == null ? BasicResultVO.fail("订单不存在") : BasicResultVO.success(row);
    }

    @PostMapping("/mock/advance")
    @ApiOperation("Mock 推进订单状态（按状态机合法转移）")
    @RequireRole({"ADMIN", "OPERATOR"})
    public BasicResultVO advanceOrder(@RequestParam String orderNo) {
        if (logisticsService.getOrder(orderNo) == null) {
            return BasicResultVO.fail("订单不存在");
        }
        LogisticsTrack nextTrack = logisticsService.advance(orderNo);
        if (nextTrack == null) {
            return BasicResultVO.fail("订单已终态，无法推进");
        }
        return BasicResultVO.success(nextTrack);
    }

    @GetMapping({"/track", "/track/"})
    @ApiOperation("查询物流轨迹（空 orderNo 兼容，避免 404）")
    public BasicResultVO trackEmpty() {
        return BasicResultVO.success(Collections.emptyList());
    }

    @GetMapping({"/order", "/order/"})
    @ApiOperation("查询物流订单（空 orderNo 兼容，避免 404）")
    public BasicResultVO orderEmpty() {
        return BasicResultVO.success(null);
    }

    @GetMapping("/track/{orderNo}")
    @ApiOperation("查询物流轨迹（PRD 8.1）")
    public BasicResultVO track(@PathVariable String orderNo) {
        return BasicResultVO.success(logisticsService.tracks(orderNo));
    }

    @GetMapping("/nodes")
    @ApiOperation("查询所有物流节点（状态机可视化）")
    public BasicResultVO listNodes() {
        return BasicResultVO.success(logisticsService.listNodes());
    }

    @GetMapping("/order/list")
    @ApiOperation("查询所有订单（DB 分页）")
    public BasicResultVO listOrders(@RequestParam(required = false) String orderNo,
                                    @RequestParam(required = false) String status,
                                    @RequestParam(defaultValue = "1") Integer page,
                                    @RequestParam(defaultValue = "10") Integer perPage) {
        return BasicResultVO.success(logisticsService.listOrders(orderNo, status, page, perPage));
    }
}
