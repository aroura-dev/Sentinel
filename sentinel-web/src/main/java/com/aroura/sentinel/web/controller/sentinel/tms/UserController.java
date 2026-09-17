package com.aroura.sentinel.web.controller.sentinel.tms;

import com.aroura.sentinel.common.vo.BasicResultVO;
import com.aroura.sentinel.web.annotation.RequireRole;
import com.aroura.sentinel.web.service.sentinel.tms.UserService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
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
 * 用户管理接口（设置 → 用户管理）
 *
 * @author sentinel
 */
@RestController
@RequestMapping("/api/sys/user")
@Api(tags = "用户管理")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/list")
    @ApiOperation("用户分页")
    @RequireRole({"ADMIN"})
    public BasicResultVO list(@RequestParam(required = false) String keyword,
                              @RequestParam(required = false) String role,
                              @RequestParam(required = false) String status,
                              @RequestParam(defaultValue = "1") Integer page,
                              @RequestParam(defaultValue = "10") Integer perPage) {
        return BasicResultVO.success(userService.list(keyword, role, status, page, perPage));
    }

    @GetMapping("/roles")
    @ApiOperation("角色列表")
    @RequireRole({"ADMIN"})
    public BasicResultVO roles() {
        return BasicResultVO.success(userService.roles());
    }

    @PostMapping("/create")
    @ApiOperation("创建用户")
    @RequireRole({"ADMIN"})
    public BasicResultVO create(@RequestBody Map<String, Object> body) {
        return BasicResultVO.success(userService.create(
                str(body.get("username")), str(body.get("password")), str(body.get("nickname")),
                str(body.get("phone")), str(body.get("email")),
                str(body.get("role")), str(body.get("status"))));
    }

    @PutMapping("/{id}")
    @ApiOperation("修改用户资料（昵称/手机号/邮箱/角色/状态）")
    @RequireRole({"ADMIN"})
    public BasicResultVO update(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        return BasicResultVO.success(userService.update(id,
                str(body.get("nickname")), str(body.get("phone")), str(body.get("email")),
                str(body.get("role")), str(body.get("status"))));
    }

    @PostMapping("/{id}/reset-password")
    @ApiOperation("重置用户密码")
    @RequireRole({"ADMIN"})
    public BasicResultVO resetPassword(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        return BasicResultVO.success(userService.resetPassword(id, str(body.get("password"))));
    }
    @PostMapping("/{id}/toggle")
    @ApiOperation("启停用户")
    @RequireRole({"ADMIN"})
    public BasicResultVO toggle(@PathVariable Long id) {
        return BasicResultVO.success(userService.toggle(id));
    }

    private static String str(Object v) {
        return v == null ? null : String.valueOf(v);
    }
}
