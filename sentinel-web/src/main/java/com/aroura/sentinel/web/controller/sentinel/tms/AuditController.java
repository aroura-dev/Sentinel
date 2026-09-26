package com.aroura.sentinel.web.controller.sentinel.tms;

import com.aroura.sentinel.common.vo.BasicResultVO;
import com.aroura.sentinel.web.annotation.RequireRole;
import com.aroura.sentinel.web.service.sentinel.tms.AuditLogService;
import com.aroura.sentinel.web.support.TenantScopeResolver;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 操作审计接口（谁在何时对哪个业务对象做了什么）
 * <p>
 * 订单详情等业务页需要展示本业务对象的操作留痕，故放开到 ADMIN/OPERATOR/FINANCE/MERCHANT；
 * 商家角色由 {@code merchantId} 作用域限制，只能查到本商家订单/运单的操作记录。
 *
 * @author sentinel
 */
@RestController
@RequestMapping("/api/audit")
@Api(tags = "操作审计")
public class AuditController {

    private final AuditLogService auditLogService;
    private final TenantScopeResolver tenantScope;

    public AuditController(AuditLogService auditLogService, TenantScopeResolver tenantScope) {
        this.auditLogService = auditLogService;
        this.tenantScope = tenantScope;
    }

    @GetMapping("/list")
    @ApiOperation("审计日志分页（支持模块/操作人/单号/动作过滤；商家按本商家隔离）")
    @RequireRole({"ADMIN", "OPERATOR", "FINANCE", "MERCHANT"})
    public BasicResultVO list(@RequestParam(required = false) String module,
                              @RequestParam(required = false) String operator,
                              @RequestParam(required = false) String targetNo,
                              @RequestParam(required = false) String action,
                              @RequestParam(required = false) String start,
                              @RequestParam(required = false) String end,
                              @RequestParam(defaultValue = "1") Integer page,
                              @RequestParam(defaultValue = "10") Integer perPage) {
        // MERCHANT 未绑定商家档案时 currentScope() 直接拒绝，不再退化成「看全平台」
        Long merchantScope = tenantScope.currentScope();
        return BasicResultVO.success(auditLogService.list(module, operator, targetNo, action, start, end,
                merchantScope, page, perPage));
    }
}
