package com.aroura.sentinel.web.controller.sentinel.tms;

import com.aroura.sentinel.common.vo.BasicResultVO;
import com.aroura.sentinel.web.annotation.RequireRole;
import com.aroura.sentinel.web.service.sentinel.tms.CarrierService;
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
 * 承运商主数据接口
 *
 * @author sentinel
 */
@RestController
@RequestMapping("/api/tms/carrier")
@Api(tags = "TMS 承运商主数据")
public class CarrierController {

    private final CarrierService carrierService;

    public CarrierController(CarrierService carrierService) {
        this.carrierService = carrierService;
    }

    @GetMapping("/list")
    @ApiOperation("承运商分页")
    @RequireRole({"ADMIN", "OPERATOR", "FINANCE", "MERCHANT"})
    public BasicResultVO list(@RequestParam(required = false) String keyword,
                              @RequestParam(defaultValue = "1") Integer page,
                              @RequestParam(defaultValue = "10") Integer perPage) {
        return BasicResultVO.success(carrierService.list(keyword, page, perPage));
    }

    @GetMapping("/all")
    @ApiOperation("全部启用承运商（下拉）")
    @RequireRole({"ADMIN", "OPERATOR", "FINANCE", "MERCHANT"})
    public BasicResultVO all() {
        return BasicResultVO.success(carrierService.listAll());
    }

    @GetMapping("/{id}")
    @ApiOperation("承运商详情")
    @RequireRole({"ADMIN", "OPERATOR", "FINANCE", "MERCHANT"})
    public BasicResultVO detail(@PathVariable Long id) {
        return BasicResultVO.success(carrierService.detail(id));
    }

    @PostMapping
    @ApiOperation("新增承运商")
    @RequireRole({"ADMIN", "OPERATOR"})
    public BasicResultVO create(@RequestBody Map<String, Object> body) {
        return BasicResultVO.success(carrierService.save(body));
    }

    @PutMapping("/{id}")
    @ApiOperation("更新承运商")
    @RequireRole({"ADMIN", "OPERATOR"})
    public BasicResultVO update(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        body.put("id", id);
        return BasicResultVO.success(carrierService.save(body));
    }

    @DeleteMapping("/{id}")
    @ApiOperation("删除承运商（逻辑删除）")
    @RequireRole({"ADMIN", "OPERATOR"})
    public BasicResultVO delete(@PathVariable Long id) {
        carrierService.delete(id);
        return BasicResultVO.success(true);
    }
}
