package com.aroura.sentinel.web.controller.sentinel.tms;

import com.aroura.sentinel.common.vo.BasicResultVO;
import com.aroura.sentinel.web.annotation.RequireRole;
import com.aroura.sentinel.web.service.sentinel.tms.StocktakeService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 移库盘点接口
 *
 * @author sentinel
 */
@RestController
@RequestMapping("/api/tms/stocktake")
@Api(tags = "移库盘点")
public class StocktakeController {

    private final StocktakeService stocktakeService;

    public StocktakeController(StocktakeService stocktakeService) {
        this.stocktakeService = stocktakeService;
    }

    @GetMapping("/list")
    @ApiOperation("盘点单列表")
    @RequireRole({"ADMIN", "OPERATOR", "FINANCE"})
    public BasicResultVO list(@RequestParam(required = false) String status,
                              @RequestParam(defaultValue = "1") int page,
                              @RequestParam(defaultValue = "10") int perPage) {
        return BasicResultVO.success(stocktakeService.list(status, page, perPage));
    }

    @GetMapping("/{id}")
    @ApiOperation("盘点单详情")
    @RequireRole({"ADMIN", "OPERATOR", "FINANCE"})
    public BasicResultVO detail(@PathVariable Long id) {
        return BasicResultVO.success(stocktakeService.detail(id));
    }

    @PostMapping("/create")
    @ApiOperation("新建盘点单")
    @RequireRole({"ADMIN", "OPERATOR"})
    public BasicResultVO create(@RequestBody Map<String, Object> body) {
        Long warehouseId = body.get("warehouseId") == null ? null : Long.valueOf(String.valueOf(body.get("warehouseId")));
        Long merchantId = body.get("merchantId") == null ? null : Long.valueOf(String.valueOf(body.get("merchantId")));
        Object skuObj = body.get("skus");
        List<String> skus = new ArrayList<>();
        if (skuObj instanceof List) {
            for (Object o : (List<?>) skuObj) {
                skus.add(String.valueOf(o));
            }
        }
        return BasicResultVO.success(stocktakeService.create(
                warehouseId,
                body.get("scope") == null ? "ALL" : String.valueOf(body.get("scope")),
                merchantId,
                body.get("remark") == null ? null : String.valueOf(body.get("remark")),
                skus));
    }

    @PostMapping("/{id}/save-count")
    @ApiOperation("保存实盘数量")
    @RequireRole({"ADMIN", "OPERATOR"})
    public BasicResultVO saveCount(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Object itemsObj = body.get("items");
        List<Map<String, Object>> items = new ArrayList<>();
        if (itemsObj instanceof List) {
            for (Object o : (List<?>) itemsObj) {
                if (o instanceof Map) {
                    items.add((Map<String, Object>) o);
                }
            }
        }
        return BasicResultVO.success(stocktakeService.saveCount(id, items));
    }

    @PostMapping("/{id}/finish")
    @ApiOperation("完成盘点并入账")
    @RequireRole({"ADMIN", "OPERATOR"})
    public BasicResultVO finish(@PathVariable Long id) {
        return BasicResultVO.success(stocktakeService.finish(id));
    }

    @PostMapping("/{id}/cancel")
    @ApiOperation("取消盘点")
    @RequireRole({"ADMIN", "OPERATOR"})
    public BasicResultVO cancel(@PathVariable Long id) {
        return BasicResultVO.success(stocktakeService.cancel(id));
    }
}