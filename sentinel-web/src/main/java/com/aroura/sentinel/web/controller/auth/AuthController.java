package com.aroura.sentinel.web.controller.auth;

import com.aroura.sentinel.common.enums.RespStatusEnum;
import com.aroura.sentinel.common.vo.BasicResultVO;
import com.aroura.sentinel.web.service.AuthService;
import com.aroura.sentinel.web.vo.CurrentUserVO;
import com.aroura.sentinel.web.vo.LoginResultVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 多角色登录认证
 * <p>
 * 账号来自 sentinel_user 表（bcrypt + 角色），登录成功返回 token + username + role + nickname。
 *
 * @author sentinel
 */
@RestController
@RequestMapping("/api/auth")
@Api(tags = "Sentinel 登录认证")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    @ApiOperation("登录")
    public BasicResultVO login(@RequestParam String username, @RequestParam String password) {
        LoginResultVO result = authService.login(username, password);
        if (result == null) {
            return BasicResultVO.fail(RespStatusEnum.CLIENT_BAD_PARAMETERS, "用户名或密码错误");
        }
        // 保留 token/username 两个 key，兼容旧前端；P5 前端切到 role
        Map<String, Object> data = new HashMap<>(4);
        data.put("token", result.getToken());
        data.put("username", result.getUsername());
        data.put("role", result.getRole());
        data.put("nickname", result.getNickname());
        return BasicResultVO.success(data);
    }

    @PostMapping("/logout")
    @ApiOperation("退出登录")
    public BasicResultVO logout(@RequestHeader(value = "Authorization", required = false) String authorization) {
        authService.logout(authorization);
        return BasicResultVO.success(true);
    }

    @GetMapping("/me")
    @ApiOperation("当前登录用户")
    public BasicResultVO me(@RequestHeader(value = "Authorization", required = false) String authorization) {
        CurrentUserVO user = authService.me(authorization);
        if (user == null) {
            return BasicResultVO.fail(RespStatusEnum.CLIENT_BAD_PARAMETERS, "未登录或登录已过期");
        }
        return BasicResultVO.success(user);
    }
}
