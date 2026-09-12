package com.aroura.sentinel.web.controller.sentinel.tms;

import com.aroura.sentinel.common.vo.BasicResultVO;
import com.aroura.sentinel.web.annotation.RequireRole;
import com.aroura.sentinel.web.config.AuthInterceptor;
import com.aroura.sentinel.web.service.sentinel.tms.RoleService;
import com.aroura.sentinel.web.vo.CurrentUserVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 角色权限接口（设置 → 角色权限）+ 当前用户可见菜单
 *
 * @author sentinel
 */
@RestController
@RequestMapping("/api/sys")
@Api(tags = "角色权限")
public class RoleController {

    private final RoleService roleService;

    public RoleController(RoleService roleService) {
        this.roleService = roleService;
    }

    @GetMapping("/role/list")
    @ApiOperation("角色列表")
    @RequireRole({"ADMIN"})
    public BasicResultVO list() {
        return BasicResultVO.success(roleService.list());
    }

    @PostMapping("/role/create")
    @ApiOperation("创建角色")
    @RequireRole({"ADMIN"})
    public BasicResultVO create(@RequestBody Map<String, Object> body) {
        return BasicResultVO.success(roleService.create(
                str(body.get("code")), str(body.get("name")), str(body.get("description"))));
    }

    @PutMapping("/role/{id}")
    @ApiOperation("修改角色")
    @RequireRole({"ADMIN"})
    public BasicResultVO update(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        return BasicResultVO.success(roleService.update(id,
                str(body.get("name")), str(body.get("description"))));
    }

    @PostMapping("/role/{id}/toggle")
    @ApiOperation("启停角色")
    @RequireRole({"ADMIN"})
    public BasicResultVO toggle(@PathVariable Long id) {
        return BasicResultVO.success(roleService.toggle(id));
    }

    @GetMapping("/role/{code}/menus")
    @ApiOperation("角色已授权菜单")
    @RequireRole({"ADMIN"})
    public BasicResultVO menus(@PathVariable String code) {
        return BasicResultVO.success(roleService.menus(code));
    }

    @PostMapping("/role/{code}/menus")
    @ApiOperation("保存角色菜单授权（全量覆盖）")
    @RequireRole({"ADMIN"})
    public BasicResultVO saveMenus(@PathVariable String code, @RequestBody Map<String, Object> body) {
        Object paths = body.get("paths");
        List<String> list = paths instanceof List
                ? ((List<?>) paths).stream().map(String::valueOf).collect(java.util.stream.Collectors.toList())
                : Collections.emptyList();
        return BasicResultVO.success(roleService.saveMenus(code, list));
    }

    @GetMapping("/me/permissions")
    @ApiOperation("当前用户可访问菜单路径")
    public BasicResultVO myPermissions(HttpServletRequest request) {
        Object attr = request.getAttribute(AuthInterceptor.CURRENT_USER_ATTR);
        String role = (attr instanceof CurrentUserVO) ? ((CurrentUserVO) attr).getRole() : null;
        if (role == null) {
            role = "ADMIN";
        }
        return BasicResultVO.success(roleService.myPermissions(role));
    }

    private static String str(Object v) {
        return v == null ? null : String.valueOf(v);
    }
}
