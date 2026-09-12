package com.aroura.sentinel.web.controller.sentinel.tms;

import com.aroura.sentinel.common.vo.BasicResultVO;
import com.aroura.sentinel.web.annotation.RequireRole;
import com.aroura.sentinel.web.service.sentinel.tms.MerchantService;
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
 * 商家（卖家）主数据接口
 *
 * @author sentinel
 */
@RestController
@RequestMapping("/api/tms/merchant")
@Api(tags = "TMS 商家主数据")
public class MerchantController {

    private final MerchantService merchantService;

    public MerchantController(MerchantService merchantService) {
        this.merchantService = merchantService;
    }

    @GetMapping("/list")
    @ApiOperation("商家分页")
    @RequireRole({"ADMIN", "OPERATOR", "FINANCE", "MERCHANT"})
    public BasicResultVO list(@RequestParam(required = false) String keyword,
                              @RequestParam(defaultValue = "1") Integer page,
                              @RequestParam(defaultValue = "10") Integer perPage) {
        return BasicResultVO.success(merchantService.list(keyword, page, perPage));
    }

    @GetMapping("/all")
    @ApiOperation("全部启用商家（下拉）")
    @RequireRole({"ADMIN", "OPERATOR", "FINANCE", "MERCHANT"})
    public BasicResultVO all() {
        return BasicResultVO.success(merchantService.listAll());
    }

    @GetMapping("/{id}")
    @ApiOperation("商家详情")
    @RequireRole({"ADMIN", "OPERATOR", "FINANCE", "MERCHANT"})
    public BasicResultVO detail(@PathVariable Long id) {
        return BasicResultVO.success(merchantService.detail(id));
    }

    @PostMapping
    @ApiOperation("新增商家")
    @RequireRole({"ADMIN", "OPERATOR"})
    public BasicResultVO create(@RequestBody Map<String, Object> body) {
        return BasicResultVO.success(merchantService.save(body));
    }

    @PutMapping("/{id}")
    @ApiOperation("更新商家")
    @RequireRole({"ADMIN", "OPERATOR"})
    public BasicResultVO update(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        body.put("id", id);
        return BasicResultVO.success(merchantService.save(body));
    }

    @DeleteMapping("/{id}")
    @ApiOperation("删除商家（逻辑删除）")
    @RequireRole({"ADMIN", "OPERATOR"})
    public BasicResultVO delete(@PathVariable Long id) {
        merchantService.delete(id);
        return BasicResultVO.success(true);
    }
}
