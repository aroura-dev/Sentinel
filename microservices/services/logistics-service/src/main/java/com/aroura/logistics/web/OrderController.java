package com.aroura.logistics.web;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import com.aroura.sentinel.web.annotation.RequireRole;
import com.aroura.sentinel.web.vo.CurrentUserVO;
import com.aroura.logistics.service.OrderQueryService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 订单查询接口（演示 MERCHANT 行级隔离：各角色只见权限内订单）。
 * 身份由网关注入的 X-User-Name/X-User-Role 经共享 Filter 还原到 request attribute。
 *
 * @author sentinel-ms
 */
@RestController
@RequestMapping("/api/logistics/orders")
@RequireRole({"ADMIN", "OPERATOR", "MERCHANT", "CUSTOMER_SERVICE", "FINANCE"})
public class OrderController {

    @Autowired
    private OrderQueryService orderQueryService;

    @GetMapping
    public Map<String, Object> list(HttpServletRequest request,
                                    @RequestParam(defaultValue = "1") int page,
                                    @RequestParam(defaultValue = "20") int size) {
        Object attr = request.getAttribute("currentUser");
        String username = attr instanceof CurrentUserVO ? ((CurrentUserVO) attr).getUsername() : null;
        String role = attr instanceof CurrentUserVO ? ((CurrentUserVO) attr).getRole() : null;
        Map<String, Object> resp = orderQueryService.list(username, role, page, size);
        resp.put("viewer", username);
        resp.put("role", role);
        return resp;
    }
}
