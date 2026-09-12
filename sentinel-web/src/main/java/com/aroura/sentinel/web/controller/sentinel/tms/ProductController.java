package com.aroura.sentinel.web.controller.sentinel.tms;

import com.aroura.sentinel.common.vo.BasicResultVO;
import com.aroura.sentinel.web.annotation.RequireRole;
import com.aroura.sentinel.web.config.AuthInterceptor;
import com.aroura.sentinel.web.service.sentinel.tms.MerchantService;
import com.aroura.sentinel.web.service.sentinel.tms.ProductService;
import com.aroura.sentinel.web.vo.CurrentUserVO;
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

import javax.servlet.http.HttpServletRequest;
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
    private final MerchantService merchantService;

    public ProductController(ProductService productService, MerchantService merchantService) {
        this.productService = productService;
        this.merchantService = merchantService;
    }

    @GetMapping("/list")
    @ApiOperation("商品分页（MERCHANT 限定自己商家）")
    @RequireRole({"ADMIN", "OPERATOR", "FINANCE", "MERCHANT"})
    public BasicResultVO list(@RequestParam(required = false) Long merchantId,
                              @RequestParam(required = false) String keyword,
                              @RequestParam(defaultValue = "1") Integer page,
                              @RequestParam(defaultValue = "10") Integer perPage,
                              HttpServletRequest request) {
        Long forceMerchantId = resolveMerchantScope(request);
        return BasicResultVO.success(productService.list(merchantId, forceMerchantId, keyword, page, perPage));
    }

    @GetMapping("/by-merchant")
    @ApiOperation("按商家查商品（下拉）")
    @RequireRole({"ADMIN", "OPERATOR", "FINANCE", "MERCHANT"})
    public BasicResultVO byMerchant(@RequestParam Long merchantId, HttpServletRequest request) {
        Long forceMerchantId = resolveMerchantScope(request);
        if (forceMerchantId != null && !forceMerchantId.equals(merchantId)) {
            return BasicResultVO.fail("无权查看其他商家商品");
        }
        return BasicResultVO.success(productService.listByMerchant(merchantId));
    }

    @GetMapping("/{id}")
    @ApiOperation("商品详情")
    @RequireRole({"ADMIN", "OPERATOR", "FINANCE", "MERCHANT"})
    public BasicResultVO detail(@PathVariable Long id) {
        return BasicResultVO.success(productService.detail(id));
    }

    @PostMapping
    @ApiOperation("新增商品")
    @RequireRole({"ADMIN", "OPERATOR", "MERCHANT"})
    public BasicResultVO create(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        Long forceMerchantId = resolveMerchantScope(request);
        return BasicResultVO.success(productService.save(body, forceMerchantId));
    }

    @PutMapping("/{id}")
    @ApiOperation("更新商品")
    @RequireRole({"ADMIN", "OPERATOR", "MERCHANT"})
    public BasicResultVO update(@PathVariable Long id, @RequestBody Map<String, Object> body, HttpServletRequest request) {
        body.put("id", id);
        Long forceMerchantId = resolveMerchantScope(request);
        return BasicResultVO.success(productService.save(body, forceMerchantId));
    }

    @DeleteMapping("/{id}")
    @ApiOperation("删除商品（逻辑删除）")
    @RequireRole({"ADMIN", "OPERATOR", "MERCHANT"})
    public BasicResultVO delete(@PathVariable Long id) {
        productService.delete(id);
        return BasicResultVO.success(true);
    }

    /**
     * MERCHANT 角色解析其商家ID（权限隔离）；其他角色返回 null 不限制
     */
    private Long resolveMerchantScope(HttpServletRequest request) {
        Object attr = request.getAttribute(AuthInterceptor.CURRENT_USER_ATTR);
        if (attr instanceof CurrentUserVO) {
            CurrentUserVO user = (CurrentUserVO) attr;
            if ("MERCHANT".equals(user.getRole())) {
                Map<String, Object> merchant = merchantService.findByUsername(user.getUsername());
                if (merchant != null) {
                    return Long.valueOf(String.valueOf(merchant.get("id")));
                }
            }
        }
        return null;
    }
}
