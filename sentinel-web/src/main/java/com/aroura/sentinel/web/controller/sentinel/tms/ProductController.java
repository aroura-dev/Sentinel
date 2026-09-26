package com.aroura.sentinel.web.controller.sentinel.tms;

import com.aroura.sentinel.common.vo.BasicResultVO;
import com.aroura.sentinel.web.annotation.RequireRole;
import com.aroura.sentinel.web.service.sentinel.tms.ProductService;
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
 * 商品 SKU 接口（MERCHANT 角色仅能操作自己商家商品）
 *
 * @author sentinel
 */
@RestController
@RequestMapping("/api/tms/product")
@Api(tags = "TMS 商品 SKU")
public class ProductController {

    private final ProductService productService;
    private final TenantScopeResolver tenantScope;

    public ProductController(ProductService productService, TenantScopeResolver tenantScope) {
        this.productService = productService;
        this.tenantScope = tenantScope;
    }

    @GetMapping("/list")
    @ApiOperation("商品分页（MERCHANT 限定自己商家）")
    @RequireRole({"ADMIN", "OPERATOR", "FINANCE", "MERCHANT"})
    public BasicResultVO list(@RequestParam(required = false) Long merchantId,
                              @RequestParam(required = false) String keyword,
                              @RequestParam(defaultValue = "1") Integer page,
                              @RequestParam(defaultValue = "10") Integer perPage) {
        return BasicResultVO.success(
                productService.list(merchantId, tenantScope.currentScope(), keyword, page, perPage));
    }

    @GetMapping("/by-merchant")
    @ApiOperation("按商家查商品（下拉）")
    @RequireRole({"ADMIN", "OPERATOR", "FINANCE", "MERCHANT"})
    public BasicResultVO byMerchant(@RequestParam Long merchantId) {
        // MERCHANT 一律落到自己的商家（入参静默忽略，不报错）；平台角色按传入值
        return BasicResultVO.success(productService.listByMerchant(tenantScope.normalizeRequested(merchantId)));
    }

    @GetMapping("/{id}")
    @ApiOperation("商品详情")
    @RequireRole({"ADMIN", "OPERATOR", "FINANCE", "MERCHANT"})
    public BasicResultVO detail(@PathVariable Long id) {
        Map<String, Object> row = productService.detail(id);
        tenantScope.assertAccessible(merchantIdOf(row), "商品");
        return BasicResultVO.success(row);
    }

    @PostMapping
    @ApiOperation("新增商品")
    @RequireRole({"ADMIN", "OPERATOR", "MERCHANT"})
    public BasicResultVO create(@RequestBody Map<String, Object> body) {
        return BasicResultVO.success(productService.save(body, tenantScope.normalizeRequested(null)));
    }

    @PutMapping("/{id}")
    @ApiOperation("更新商品")
    @RequireRole({"ADMIN", "OPERATOR", "MERCHANT"})
    public BasicResultVO update(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        // 先取回原记录断言归属，避免"改走"他人商品；商家归属不随普通编辑变更
        Map<String, Object> existing = productService.detail(id);
        tenantScope.assertAccessible(merchantIdOf(existing), "商品");
        body.put("id", id);
        return BasicResultVO.success(productService.save(body, tenantScope.normalizeRequested(null)));
    }

    @DeleteMapping("/{id}")
    @ApiOperation("删除商品（逻辑删除）")
    @RequireRole({"ADMIN", "OPERATOR", "MERCHANT"})
    public BasicResultVO delete(@PathVariable Long id) {
        Map<String, Object> existing = productService.detail(id);
        tenantScope.assertAccessible(merchantIdOf(existing), "商品");
        productService.delete(id);
        return BasicResultVO.success(true);
    }

    private static Long merchantIdOf(Map<String, Object> row) {
        if (row == null || row.get("merchant_id") == null) {
            return null;
        }
        return Long.valueOf(String.valueOf(row.get("merchant_id")));
    }
}
