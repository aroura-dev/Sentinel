package com.aroura.sentinel.web.controller.sentinel.tms;

import com.aroura.sentinel.common.vo.BasicResultVO;
import com.aroura.sentinel.web.annotation.RequireRole;
import com.aroura.sentinel.web.service.sentinel.tms.InventoryService;
import com.aroura.sentinel.web.support.TenantScopeResolver;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 库存台账接口
 *
 * @author sentinel
 */
@RestController
@RequestMapping("/api/tms/inventory")
@Api(tags = "库存台账")
public class InventoryController {

    private final InventoryService inventoryService;
    private final TenantScopeResolver tenantScope;

    public InventoryController(InventoryService inventoryService, TenantScopeResolver tenantScope) {
        this.inventoryService = inventoryService;
        this.tenantScope = tenantScope;
    }

    @GetMapping("/list")
    @ApiOperation("SKU 库存列表")
    @RequireRole({"ADMIN", "OPERATOR", "MERCHANT", "FINANCE"})
    public BasicResultVO list(@RequestParam(required = false) String sku,
                              @RequestParam(required = false) Long merchantId,
                              @RequestParam(defaultValue = "1") int page,
                              @RequestParam(defaultValue = "10") int perPage) {
        // merchantId 是客户端可控入参，此前直接透传进 SQL —— 商家传 ?merchantId=别家 即可越权。
        // 现统一经归一化：MERCHANT 恒为自己的商家，平台角色保留代查看能力。
        return BasicResultVO.success(
                inventoryService.list(sku, tenantScope.normalizeRequested(merchantId), page, perPage));
    }

    @GetMapping("/flow")
    @ApiOperation("出入库流水")
    @RequireRole({"ADMIN", "OPERATOR", "MERCHANT", "FINANCE"})
    public BasicResultVO flow(@RequestParam(required = false) String sku,
                              @RequestParam(defaultValue = "1") int page,
                              @RequestParam(defaultValue = "10") int perPage) {
        return BasicResultVO.success(inventoryService.flow(sku, tenantScope.currentScope(), page, perPage));
    }

    @PostMapping("/adjust")
    @ApiOperation("出入库登记")
    @RequireRole({"ADMIN", "OPERATOR"})
    public BasicResultVO adjust(@RequestBody Map<String, Object> body) {
        return BasicResultVO.success(inventoryService.adjust(
                String.valueOf(body.get("sku")),
                body.get("bizNo") == null ? null : String.valueOf(body.get("bizNo")),
                String.valueOf(body.get("bizType")),
                Integer.parseInt(String.valueOf(body.get("qty"))),
                body.get("warehouseId") == null ? null : Long.valueOf(String.valueOf(body.get("warehouseId")))));
    }
}
