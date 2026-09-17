package com.aroura.sentinel.web.controller.auth;

import com.aroura.sentinel.common.enums.RespStatusEnum;
import com.aroura.sentinel.common.vo.BasicResultVO;
import com.aroura.sentinel.web.service.AuthService;
import com.aroura.sentinel.web.service.AvatarService;
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
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;

import java.util.HashMap;
import java.util.Map;

/**
 * 多角色登录认证。账号来自 sentinel_user（bcrypt + 角色），手机验证码走腾讯云短信，邮箱验证码走 SMTP。
 */
@RestController
@RequestMapping("/api/auth")
@Api(tags = "Sentinel 登录认证")
public class AuthController {

    private final AuthService authService;
    private final AvatarService avatarService;

    public AuthController(AuthService authService, AvatarService avatarService) {
        this.authService = authService;
        this.avatarService = avatarService;
    }

    @PostMapping("/login")
    @ApiOperation("用户名密码登录")
    public BasicResultVO login(@RequestParam String username, @RequestParam String password) {
        LoginResultVO result = authService.login(username, password);
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

    @PostMapping("/avatar")
    @ApiOperation("上传当前用户头像")
    public BasicResultVO updateAvatar(@RequestParam("file") MultipartFile file, HttpServletRequest request) {
        CurrentUserVO user = authService.resolveCurrentUser(request);
        if (user == null) {
            return BasicResultVO.fail(RespStatusEnum.CLIENT_BAD_PARAMETERS, "未登录或登录已过期");
        }
        String avatar = avatarService.upload(user.getUsername(), file);
        if (avatar == null) {
            return BasicResultVO.fail(RespStatusEnum.CLIENT_BAD_PARAMETERS,
                    "头像上传失败：仅支持 JPG/PNG/GIF/WebP，且大小不超过 2MB");
        }
        Map<String, Object> data = new HashMap<String, Object>(2);
        data.put("avatar", avatar);
        return BasicResultVO.success(data);
    }

    @PostMapping("/sms/send")
    @ApiOperation("发送手机验证码（scene=login/register/reset）")
    public BasicResultVO smsSend(@RequestParam String phone,
                                 @RequestParam(value = "scene", required = false, defaultValue = "login") String scene) {
        Map<String, Object> result = authService.sendSmsCode(phone, scene);
        if (result == null) {
            return BasicResultVO.fail(RespStatusEnum.CLIENT_BAD_PARAMETERS,
                    "验证码发送失败：手机号状态不符、发送过于频繁或短信服务未配置");
        }
        return BasicResultVO.success(result);
    }

    @PostMapping("/sms/login")
    @ApiOperation("手机验证码登录")
    public BasicResultVO smsLogin(@RequestParam String phone,
                                  @RequestParam String code,
                                  @RequestParam(value = "remember", required = false) Boolean remember) {
        LoginResultVO result = authService.loginByPhone(phone, code, Boolean.TRUE.equals(remember));
        if (result == null) {
            return BasicResultVO.fail(RespStatusEnum.CLIENT_BAD_PARAMETERS, "验证码错误、已过期或手机号未注册");
        }
        return BasicResultVO.success(tokenData(result));
    }

    @PostMapping("/sms/register")
    @ApiOperation("手机验证码注册")
    public BasicResultVO smsRegister(@RequestParam String phone,
                                     @RequestParam String code,
                                     @RequestParam String password,
                                     @RequestParam(value = "nickname", required = false) String nickname) {
        Map<String, Object> result = authService.registerByPhone(phone, code, password, nickname);
        if (result == null) {
            return BasicResultVO.fail(RespStatusEnum.CLIENT_BAD_PARAMETERS, "注册失败：手机号已注册、验证码错误或密码不合法");
        }
        return BasicResultVO.success(result);
    }

    @PostMapping("/sms/reset")
    @ApiOperation("手机验证码重置密码")
    public BasicResultVO smsReset(@RequestParam String phone,
                                  @RequestParam String code,
                                  @RequestParam String newPassword) {
        if (!authService.resetPasswordByPhone(phone, code, newPassword)) {
            return BasicResultVO.fail(RespStatusEnum.CLIENT_BAD_PARAMETERS, "重置失败：验证码错误或手机号未注册");
        }
        return BasicResultVO.success(true);
    }

    @PostMapping("/email/send")
    @ApiOperation("发送邮箱验证码（scene=login/register/reset）")
    public BasicResultVO emailSend(@RequestParam String email,
                                   @RequestParam(value = "scene", required = false, defaultValue = "login") String scene) {
        Map<String, Object> result = authService.sendEmailCode(email, scene);
        if (result == null) {
            return BasicResultVO.fail(RespStatusEnum.CLIENT_BAD_PARAMETERS,
                    "验证码发送失败：邮箱状态不符、发送过于频繁或邮件服务未配置");
        }
        return BasicResultVO.success(result);
    }

    @PostMapping("/email/login")
    @ApiOperation("邮箱验证码登录")
    public BasicResultVO emailLogin(@RequestParam String email,
                                    @RequestParam String code,
                                    @RequestParam(value = "remember", required = false) Boolean remember) {
        LoginResultVO result = authService.loginByEmail(email, code, Boolean.TRUE.equals(remember));
        if (result == null) {
            return BasicResultVO.fail(RespStatusEnum.CLIENT_BAD_PARAMETERS, "验证码错误、已过期或邮箱未注册");
        }
        return BasicResultVO.success(tokenData(result));
    }

    @PostMapping("/email/register")
    @ApiOperation("邮箱验证码注册")
    public BasicResultVO emailRegister(@RequestParam String email,
                                       @RequestParam String code,
                                       @RequestParam String password,
                                       @RequestParam(value = "nickname", required = false) String nickname) {
        Map<String, Object> result = authService.registerByEmail(email, code, password, nickname);
        if (result == null) {
            return BasicResultVO.fail(RespStatusEnum.CLIENT_BAD_PARAMETERS, "注册失败：邮箱已注册、验证码错误或密码不合法");
        }
        return BasicResultVO.success(result);
    }

    @PostMapping("/email/reset")
    @ApiOperation("邮箱验证码重置密码")
    public BasicResultVO emailReset(@RequestParam String email,
                                    @RequestParam String code,
                                    @RequestParam String newPassword) {
        if (!authService.resetPasswordByEmail(email, code, newPassword)) {
            return BasicResultVO.fail(RespStatusEnum.CLIENT_BAD_PARAMETERS, "重置失败：验证码错误或邮箱未注册");
        }
        return BasicResultVO.success(true);
    }

    private Map<String, Object> tokenData(LoginResultVO result) {
        Map<String, Object> data = new HashMap<String, Object>(5);
        data.put("token", result.getToken());
        data.put("username", result.getUsername());
        data.put("role", result.getRole());
        data.put("nickname", result.getNickname());
        data.put("avatar", result.getAvatar());
        return data;
    }
}