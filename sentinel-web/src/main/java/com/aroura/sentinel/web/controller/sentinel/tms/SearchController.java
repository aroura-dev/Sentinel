package com.aroura.sentinel.web.controller.sentinel.tms;

import com.aroura.sentinel.common.vo.BasicResultVO;
import com.aroura.sentinel.web.annotation.RequireRole;
import com.aroura.sentinel.web.service.sentinel.tms.SearchService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 全局搜索：订单 / 运单 / 商家 一个关键词直达
 *
 * @author sentinel
 */
@RestController
@RequestMapping("/api/tms")
@Api(tags = "TMS 全局搜索")
public class SearchController {

    private final SearchService searchService;

    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    @GetMapping("/search")
    @ApiOperation("全局搜索（订单/运单/商家）")
    @RequireRole({"ADMIN", "OPERATOR", "FINANCE", "MERCHANT", "CUSTOMER_SERVICE"})
    public BasicResultVO search(@RequestParam String keyword) {
        return BasicResultVO.success(searchService.search(keyword));
    }
}
