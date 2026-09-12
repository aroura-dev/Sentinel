package com.aroura.sentinel.web.controller.sentinel.tms;

import com.aroura.sentinel.common.vo.BasicResultVO;
import com.aroura.sentinel.web.annotation.RequireRole;
import com.aroura.sentinel.web.service.sentinel.tms.OrderReviewService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 订单审核接口（订单 → 订单审核）
 *
 * @author sentinel
 */
@RestController
@RequestMapping("/api/tms/order")
@Api(tags = "订单审核")
public class OrderReviewController {

    private final OrderReviewService reviewService;

    public OrderReviewController(OrderReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @GetMapping("/review-list")
    @ApiOperation("待审核/已驳回订单分页")
    @RequireRole({"ADMIN", "OPERATOR"})
    public BasicResultVO list(@RequestParam(required = false) String status,
                              @RequestParam(required = false) String keyword,
                              @RequestParam(defaultValue = "1") Integer page,
                              @RequestParam(defaultValue = "10") Integer perPage) {
        return BasicResultVO.success(reviewService.list(status, keyword, page, perPage));
    }

    @PostMapping("/{orderNo}/review")
    @ApiOperation("审核订单（approve=true 通过 / false 驳回+reason）")
    @RequireRole({"ADMIN", "OPERATOR"})
    public BasicResultVO review(@PathVariable String orderNo,
                                @RequestParam(defaultValue = "true") Boolean approve,
                                @RequestParam(required = false) String reason) {
        return BasicResultVO.success(reviewService.review(orderNo, approve, reason));
    }
}
