package com.java3y.austin.web.controller.sentinel.tms;

import com.java3y.austin.common.vo.BasicResultVO;
import com.java3y.austin.web.annotation.RequireRole;
import com.java3y.austin.web.config.AuthInterceptor;
import com.java3y.austin.web.service.sentinel.tms.AuditLogService;
import com.java3y.austin.web.service.sentinel.tms.MerchantService;
import com.java3y.austin.web.vo.CurrentUserVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

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
    private final MerchantService merchantService;

    public AuditController(AuditLogService auditLogService, MerchantService merchantService) {
        this.auditLogService = auditLogService;
        this.merchantService = merchantService;
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
                              @RequestParam(defaultValue = "10") Integer perPage,
                              HttpServletRequest request) {
        Long merchantScope = null;
        Object attr = request.getAttribute(AuthInterceptor.CURRENT_USER_ATTR);
        if (attr instanceof CurrentUserVO) {
            CurrentUserVO user = (CurrentUserVO) attr;
            if ("MERCHANT".equals(user.getRole())) {
                Map<String, Object> merchant = merchantService.findByUsername(user.getUsername());
                if (merchant != null) {
                    merchantScope = Long.valueOf(String.valueOf(merchant.get("id")));
                }
            }
        }
        return BasicResultVO.success(auditLogService.list(module, operator, targetNo, action, start, end, merchantScope, page, perPage));
    }
}
