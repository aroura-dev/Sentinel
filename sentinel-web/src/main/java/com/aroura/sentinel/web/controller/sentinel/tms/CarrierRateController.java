package com.aroura.sentinel.web.controller.sentinel.tms;

import com.aroura.sentinel.common.vo.BasicResultVO;
import com.aroura.sentinel.web.annotation.RequireRole;
import com.aroura.sentinel.web.service.sentinel.tms.CarrierRateService;
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
 * 运费价卡接口
 *
 * @author sentinel
 */
@RestController
@RequestMapping("/api/tms/rate")
@Api(tags = "TMS 运费价卡")
public class CarrierRateController {

    private final CarrierRateService rateService;

    public CarrierRateController(CarrierRateService rateService) {
        this.rateService = rateService;
    }

    @GetMapping("/list")
    @ApiOperation("价卡分页")
    @RequireRole({"ADMIN", "OPERATOR", "FINANCE", "MERCHANT"})
    public BasicResultVO list(@RequestParam(required = false) Long channelId,
                              @RequestParam(required = false) String zone,
                              @RequestParam(defaultValue = "1") Integer page,
                              @RequestParam(defaultValue = "10") Integer perPage) {
        return BasicResultVO.success(rateService.list(channelId, zone, page, perPage));
    }

    @GetMapping("/by-channel")
    @ApiOperation("按渠道查全部价卡")
    @RequireRole({"ADMIN", "OPERATOR", "FINANCE", "MERCHANT"})
    public BasicResultVO byChannel(@RequestParam Long channelId) {
        return BasicResultVO.success(rateService.listByChannel(channelId));
    }

    @GetMapping("/{id}")
    @ApiOperation("价卡详情")
    @RequireRole({"ADMIN", "OPERATOR", "FINANCE", "MERCHANT"})
    public BasicResultVO detail(@PathVariable Long id) {
        return BasicResultVO.success(rateService.detail(id));
    }

    @PostMapping
    @ApiOperation("新增价卡")
    @RequireRole({"ADMIN", "OPERATOR"})
    public BasicResultVO create(@RequestBody Map<String, Object> body) {
        return BasicResultVO.success(rateService.save(body));
    }

    @PutMapping("/{id}")
    @ApiOperation("更新价卡")
    @RequireRole({"ADMIN", "OPERATOR"})
    public BasicResultVO update(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        body.put("id", id);
        return BasicResultVO.success(rateService.save(body));
    }

    @DeleteMapping("/{id}")
    @ApiOperation("删除价卡（逻辑删除）")
    @RequireRole({"ADMIN", "OPERATOR"})
    public BasicResultVO delete(@PathVariable Long id) {
        rateService.delete(id);
        return BasicResultVO.success(true);
    }
}
