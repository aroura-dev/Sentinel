package com.aroura.sentinel.web.controller.sentinel.tms;

import com.aroura.sentinel.common.vo.BasicResultVO;
import com.aroura.sentinel.web.annotation.RequireRole;
import com.aroura.sentinel.web.service.sentinel.tms.MerchantService;
import com.aroura.sentinel.web.support.TenantScopeResolver;
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
 * <p>
 * MERCHANT 角色只能看到自己那一条商家档案，不能枚举全部商家的联系方式。
 *
 * @author sentinel
 */
@RestController
@RequestMapping("/api/tms/merchant")
@Api(tags = "TMS 商家主数据")
public class MerchantController {

    private final MerchantService merchantService;
    private final TenantScopeResolver tenantScope;

    public MerchantController(MerchantService merchantService, TenantScopeResolver tenantScope) {
        this.merchantService = merchantService;
        this.tenantScope = tenantScope;
    }

    @GetMapping("/list")
    @ApiOperation("商家分页（MERCHANT 仅见自己）")
    @RequireRole({"ADMIN", "OPERATOR", "FINANCE", "MERCHANT"})
    public BasicResultVO list(@RequestParam(required = false) String keyword,
                              @RequestParam(defaultValue = "1") Integer page,
                              @RequestParam(defaultValue = "10") Integer perPage) {
        return BasicResultVO.success(merchantService.list(keyword, tenantScope.currentScope(), page, perPage));
    }

    @GetMapping("/all")
    @ApiOperation("全部启用商家（下拉）")
    @RequireRole({"ADMIN", "OPERATOR", "FINANCE", "MERCHANT"})
    public BasicResultVO all() {
        // 该方法被前端当作"商家名字字典"用于下单页/商品页的必选下拉，故 MERCHANT 分支
        // 必须返回自己那一行而不是空集，否则这些页面直接不可用（见 MerchantDao.listAll 注释）。
        // 注意这里必须先判 isMerchant：未绑定商家的 MERCHANT 与平台角色在 currentScopeOrNull()
        // 下都返回 null，直接用会把"未绑定"当成"不限制"，反而泄露全部商家。
        if (tenantScope.isMerchant()) {
            Long own = tenantScope.currentScopeOrNull();
            return BasicResultVO.success(own == null
                    ? java.util.Collections.emptyList()
                    : merchantService.listAll(own));
        }
        return BasicResultVO.success(merchantService.listAll());
    }

    @GetMapping("/{id}")
    @ApiOperation("商家详情")
    @RequireRole({"ADMIN", "OPERATOR", "FINANCE", "MERCHANT"})
    public BasicResultVO detail(@PathVariable Long id) {
        tenantScope.assertAccessible(id, "商家");
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
