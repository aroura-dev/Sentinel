package com.aroura.sentinel.web.controller.sentinel.tms;

import com.aroura.sentinel.common.vo.BasicResultVO;
import com.aroura.sentinel.web.annotation.RequireRole;
import com.aroura.sentinel.web.service.sentinel.tms.CarrierChannelService;
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
 * 物流渠道接口
 *
 * @author sentinel
 */
@RestController
@RequestMapping("/api/tms/channel")
@Api(tags = "TMS 物流渠道")
public class CarrierChannelController {

    private final CarrierChannelService channelService;

    public CarrierChannelController(CarrierChannelService channelService) {
        this.channelService = channelService;
    }

    @GetMapping("/list")
    @ApiOperation("渠道分页")
    @RequireRole({"ADMIN", "OPERATOR", "FINANCE", "MERCHANT"})
    public BasicResultVO list(@RequestParam(required = false) Long carrierId,
                              @RequestParam(required = false) String destCountry,
                              @RequestParam(required = false) String type,
                              @RequestParam(defaultValue = "1") Integer page,
                              @RequestParam(defaultValue = "10") Integer perPage) {
        return BasicResultVO.success(channelService.list(carrierId, destCountry, type, page, perPage));
    }

    @GetMapping("/all")
    @ApiOperation("全部启用渠道（下拉）")
    @RequireRole({"ADMIN", "OPERATOR", "FINANCE", "MERCHANT"})
    public BasicResultVO all() {
        return BasicResultVO.success(channelService.listAll());
    }

    @GetMapping("/by-country")
    @ApiOperation("按目的地查可用渠道")
    @RequireRole({"ADMIN", "OPERATOR", "FINANCE", "MERCHANT"})
    public BasicResultVO byCountry(@RequestParam String destCountry) {
        return BasicResultVO.success(channelService.listByDestCountry(destCountry));
    }

    @GetMapping("/{id}")
    @ApiOperation("渠道详情")
    @RequireRole({"ADMIN", "OPERATOR", "FINANCE", "MERCHANT"})
    public BasicResultVO detail(@PathVariable Long id) {
        return BasicResultVO.success(channelService.detail(id));
    }

    @PostMapping
    @ApiOperation("新增渠道")
    @RequireRole({"ADMIN", "OPERATOR"})
    public BasicResultVO create(@RequestBody Map<String, Object> body) {
        return BasicResultVO.success(channelService.save(body));
    }

    @PutMapping("/{id}")
    @ApiOperation("更新渠道")
    @RequireRole({"ADMIN", "OPERATOR"})
    public BasicResultVO update(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        body.put("id", id);
        return BasicResultVO.success(channelService.save(body));
    }

    @DeleteMapping("/{id}")
    @ApiOperation("删除渠道（逻辑删除）")
    @RequireRole({"ADMIN", "OPERATOR"})
    public BasicResultVO delete(@PathVariable Long id) {
        channelService.delete(id);
        return BasicResultVO.success(true);
    }
}
