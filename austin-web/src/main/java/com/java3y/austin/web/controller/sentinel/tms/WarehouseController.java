package com.java3y.austin.web.controller.sentinel.tms;

import com.java3y.austin.common.vo.BasicResultVO;
import com.java3y.austin.web.annotation.RequireRole;
import com.java3y.austin.web.service.sentinel.tms.WarehouseService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.DeleteMapping;
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
 * 仓库主数据接口
 *
 * @author sentinel
 */
@RestController
@RequestMapping("/api/tms/warehouse")
@Api(tags = "TMS 仓库主数据")
public class WarehouseController {

    private final WarehouseService warehouseService;

    public WarehouseController(WarehouseService warehouseService) {
        this.warehouseService = warehouseService;
    }

    @GetMapping("/list")
    @ApiOperation("仓库分页")
    @RequireRole({"ADMIN", "OPERATOR", "FINANCE", "MERCHANT"})
    public BasicResultVO list(@RequestParam(required = false) String keyword,
                              @RequestParam(defaultValue = "1") Integer page,
                              @RequestParam(defaultValue = "10") Integer perPage) {
        return BasicResultVO.success(warehouseService.list(keyword, page, perPage));
    }

    @GetMapping("/all")
    @ApiOperation("全部启用仓库（下拉）")
    @RequireRole({"ADMIN", "OPERATOR", "FINANCE", "MERCHANT"})
    public BasicResultVO all() {
        return BasicResultVO.success(warehouseService.listAll());
    }

    @GetMapping("/{id}")
    @ApiOperation("仓库详情")
    @RequireRole({"ADMIN", "OPERATOR", "FINANCE", "MERCHANT"})
    public BasicResultVO detail(@PathVariable Long id) {
        return BasicResultVO.success(warehouseService.detail(id));
    }

    @PostMapping
    @ApiOperation("新增仓库")
    @RequireRole({"ADMIN", "OPERATOR"})
    public BasicResultVO create(@RequestBody Map<String, Object> body) {
        return BasicResultVO.success(warehouseService.save(body));
    }

    @PutMapping("/{id}")
    @ApiOperation("更新仓库")
    @RequireRole({"ADMIN", "OPERATOR"})
    public BasicResultVO update(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        body.put("id", id);
        return BasicResultVO.success(warehouseService.save(body));
    }

    @DeleteMapping("/{id}")
    @ApiOperation("删除仓库（逻辑删除）")
    @RequireRole({"ADMIN", "OPERATOR"})
    public BasicResultVO delete(@PathVariable Long id) {
        warehouseService.delete(id);
        return BasicResultVO.success(true);
    }
}
