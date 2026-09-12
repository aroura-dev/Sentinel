package com.java3y.austin.web.controller.auth;

import com.java3y.austin.common.enums.RespStatusEnum;
import com.java3y.austin.common.vo.BasicResultVO;
import com.java3y.austin.web.service.AuthService;
import com.java3y.austin.web.vo.CurrentUserVO;
import com.java3y.austin.web.vo.LoginResultVO;
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
 * 公开端点集中在 login 与 /sms/**（网关白名单）；sms 场景支持注册/登录/重置。
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
    public BasicResultVO login(@RequestParam String username,
                               @RequestParam String password,
                               @RequestParam(value = "remember", required = false) Boolean remember) {
        LoginResultVO result = authService.login(username, password, Boolean.TRUE.equals(remember));
        if (result == null) {
            return BasicResultVO.fail(RespStatusEnum.CLIENT_BAD_PARAMETERS, "用户名或密码错误");
        }
        return BasicResultVO.success(tokenData(result));
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

    @PostMapping("/sms/send")
    @ApiOperation("手机验证码-发送（scene=login 须已注册 / register 须未注册）")
    public BasicResultVO smsSend(@RequestParam String phone,
                                 @RequestParam(value = "scene", required = false, defaultValue = "login") String scene) {
        Map<String, Object> resp = authService.sendSmsCode(phone, scene);
        if (resp == null) {
            return BasicResultVO.fail(RespStatusEnum.CLIENT_BAD_PARAMETERS,
                    "验证码发送失败：手机号状态不符或发送过于频繁");
        }
        return BasicResultVO.success(resp);
    }

    @PostMapping("/sms/login")
    @ApiOperation("手机验证码-登录")
    public BasicResultVO smsLogin(@RequestParam String phone,
                                  @RequestParam String code,
                                  @RequestParam(value = "remember", required = false) Boolean remember) {
        LoginResultVO result = authService.loginByCode(phone, code, Boolean.TRUE.equals(remember));
        if (result == null) {
            return BasicResultVO.fail(RespStatusEnum.CLIENT_BAD_PARAMETERS, "验证码错误或已过期");
        }
        return BasicResultVO.success(tokenData(result));
    }

    @PostMapping("/sms/register")
    @ApiOperation("手机验证码-注册")
    public BasicResultVO smsRegister(@RequestParam String phone,
                                     @RequestParam String code,
                                     @RequestParam String password,
                                     @RequestParam(value = "nickname", required = false) String nickname) {
        Map<String, Object> data = authService.register(phone, code, password, nickname);
        if (data == null) {
            return BasicResultVO.fail(RespStatusEnum.CLIENT_BAD_PARAMETERS, "注册失败：手机号已注册、验证码错误或密码不合法");
        }
        return BasicResultVO.success(data);
    }

    @PostMapping("/sms/reset")
    @ApiOperation("手机验证码-重置密码")
    public BasicResultVO smsReset(@RequestParam String phone,
                                  @RequestParam String code,
                                  @RequestParam String newPassword) {
        if (!authService.resetPassword(phone, code, newPassword)) {
            return BasicResultVO.fail(RespStatusEnum.CLIENT_BAD_PARAMETERS, "重置失败：验证码错误或手机号未注册");
        }
        return BasicResultVO.success(true);
    }

    private Map<String, Object> tokenData(LoginResultVO result) {
        Map<String, Object> data = new HashMap<>(4);
        data.put("token", result.getToken());
        data.put("username", result.getUsername());
        data.put("role", result.getRole());
        data.put("nickname", result.getNickname());
        return data;
    }
}
