package com.aroura.sentinel.web.service;

import com.aroura.sentinel.web.dao.SentinelUserDao;
import com.aroura.sentinel.web.email.EmailCodeService;
import com.aroura.sentinel.web.sms.SmsCodeService;
import com.aroura.sentinel.web.vo.CurrentUserVO;
import com.aroura.sentinel.web.vo.LoginResultVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * 多角色登录认证服务。
 *
 * <p>认证体系：sentinel_user 表（bcrypt 密码 + 角色）+ Redis 会话 token。
 * 手机/邮箱验证码同样使用 Redis 保存，并支持登录、注册和重置密码。</p>
 */
@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    public static final String TOKEN_PREFIX = "sentinel:token:";
    public static final String SESSION_SEPARATOR = ":";

    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[\\p{IsHan}A-Za-z][\\p{IsHan}A-Za-z0-9._-]{1,31}$");

    private final StringRedisTemplate redisTemplate;
    private final SentinelUserDao userDao;
    private final SmsCodeService smsCodeService;
    private final EmailCodeService emailCodeService;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Value("${sentinel.login.token-ttl-seconds:7200}")
    private long tokenTtlSeconds;

    @Value("${auth.remember-ttl-seconds:604800}")
    private long rememberTtlSeconds;

    @Value("${auth.register-default-role:MERCHANT}")
    private String registerDefaultRole;

    public AuthService(StringRedisTemplate redisTemplate, SentinelUserDao userDao,
                       SmsCodeService smsCodeService, EmailCodeService emailCodeService) {
        this.redisTemplate = redisTemplate;
        this.userDao = userDao;
        this.smsCodeService = smsCodeService;
        this.emailCodeService = emailCodeService;
    }

    /** 用户名密码登录，默认短会话。 */
    public LoginResultVO login(String username, String password) {
        return login(username, password, false);
    }

    /** 用户名密码登录，remember 控制长效会话。 */
    public LoginResultVO login(String username, String password, boolean remember) {
        if (username == null || password == null || username.trim().isEmpty()) {
            return null;
        }
        Map<String, Object> user = userDao.findByUsername(username.trim());
        if (user == null || !isEnabled(user)) {
            return null;
        }
        Object stored = user.get("password");
        if (stored == null || !passwordEncoder.matches(password, String.valueOf(stored))) {
            return null;
        }
        return createSession(user, remember ? rememberTtlSeconds : tokenTtlSeconds);
    }
    /** 发送手机验证码：login/reset 必须已注册，register 必须未注册。 */
    public Map<String, Object> sendSmsCode(String phone, String scene) {
        if (!SmsCodeService.isValidPhone(phone)) {
            return null;
        }
        String normalizedPhone = phone.trim();
        String normalizedScene = scene == null ? "login" : scene.trim().toLowerCase();
        Map<String, Object> user = userDao.findByPhone(normalizedPhone);
        if ("register".equals(normalizedScene)) {
            if (user != null) {
                return null;
            }
        } else if (user == null || !isEnabled(user)) {
            return null;
        }
        return smsCodeService.sendCode(normalizedPhone, normalizedScene);
    }

    /** 手机验证码登录。 */
    public LoginResultVO loginByPhone(String phone, String code, boolean remember) {
        if (!SmsCodeService.isValidPhone(phone)) {
            return null;
        }
        String normalizedPhone = phone.trim();
        if (!smsCodeService.verifyCode(normalizedPhone, "login", code)) {
            return null;
        }
        Map<String, Object> user = userDao.findByPhone(normalizedPhone);
        if (user == null || !isEnabled(user)) {
            return null;
        }
        return createSession(user, remember ? rememberTtlSeconds : tokenTtlSeconds);
    }

    /** 手机验证码注册，用户名与手机号分离。 */
    public Map<String, Object> registerByPhone(String phone, String code, String username,
                                               String password, String nickname) {
        if (!SmsCodeService.isValidPhone(phone) || !isValidPassword(password)) {
            return null;
        }
        String normalizedPhone = phone.trim();
        String normalizedUsername = username == null ? "" : username.trim();
        if (!USERNAME_PATTERN.matcher(normalizedUsername).matches()
                || userDao.findByPhone(normalizedPhone) != null
                || userDao.existsByUsername(normalizedUsername)) {
            return null;
        }
        if (!smsCodeService.verifyCode(normalizedPhone, "register", code)) {
            return null;
        }
        String normalizedNickname = nickname == null || nickname.trim().isEmpty()
                ? normalizedUsername : nickname.trim();
        Long id = userDao.insert(normalizedUsername, normalizedPhone,
                passwordEncoder.encode(password), normalizedNickname, registerDefaultRole, "1");
        if (id == null) {
            return null;
        }
        Map<String, Object> result = new HashMap<String, Object>();
        result.put("username", normalizedUsername);
        result.put("role", registerDefaultRole);
        result.put("nickname", normalizedNickname);
        return result;
    }
    /** 手机验证码重置密码。 */
    public boolean resetPasswordByPhone(String phone, String code, String newPassword) {
        if (!SmsCodeService.isValidPhone(phone) || !isValidPassword(newPassword)) {
            return false;
        }
        String normalizedPhone = phone.trim();
        if (!smsCodeService.verifyCode(normalizedPhone, "reset", code)) {
            return false;
        }
        Map<String, Object> user = userDao.findByPhone(normalizedPhone);
        if (user == null) {
            return false;
        }
        Object id = user.get("id");
        long uid = id instanceof Number ? ((Number) id).longValue() : Long.parseLong(String.valueOf(id));
        userDao.updatePassword(uid, passwordEncoder.encode(newPassword));
        revokeSessions(String.valueOf(user.get("username")));
        log.info("[Auth] 手机号密码已重置 phone={}", normalizedPhone);
        return true;
    }
    /** 发送邮箱验证码：login/reset 必须已注册，register 必须未注册。 */
    public Map<String, Object> sendEmailCode(String email, String scene) {
        if (!EmailCodeService.isValidEmail(email)) {
            return null;
        }
        String normalizedEmail = email.trim().toLowerCase();
        String normalizedScene = scene == null ? "login" : scene.trim().toLowerCase();
        Map<String, Object> user = userDao.findByEmail(normalizedEmail);
        if ("register".equals(normalizedScene)) {
            if (user != null || userDao.existsByUsername(normalizedEmail)) {
                return null;
            }
        } else if (user == null || !isEnabled(user)) {
            return null;
        }
        return emailCodeService.sendCode(normalizedEmail, normalizedScene);
    }

    /** 邮箱验证码登录。 */
    public LoginResultVO loginByEmail(String email, String code, boolean remember) {
        if (!EmailCodeService.isValidEmail(email)) {
            return null;
        }
        String normalizedEmail = email.trim().toLowerCase();
        if (!emailCodeService.verifyCode(normalizedEmail, "login", code)) {
            return null;
        }
        Map<String, Object> user = userDao.findByEmail(normalizedEmail);
        if (user == null || !isEnabled(user)) {
            return null;
        }
        return createSession(user, remember ? rememberTtlSeconds : tokenTtlSeconds);
    }

    /** 邮箱验证码注册，默认用户名为邮箱。 */
    public Map<String, Object> registerByEmail(String email, String code, String password, String nickname) {
        if (!EmailCodeService.isValidEmail(email) || !isValidPassword(password)) {
            return null;
        }
        String normalizedEmail = email.trim().toLowerCase();
        if (userDao.findByEmail(normalizedEmail) != null || userDao.existsByUsername(normalizedEmail)) {
            return null;
        }
        if (!emailCodeService.verifyCode(normalizedEmail, "register", code)) {
            return null;
        }
        String normalizedNickname = nickname == null || nickname.trim().isEmpty()
                ? normalizedEmail : nickname.trim();
        Long id = userDao.insertByEmail(normalizedEmail,
                passwordEncoder.encode(password), normalizedNickname, registerDefaultRole, "1");
        if (id == null) {
            return null;
        }
        Map<String, Object> result = new HashMap<String, Object>();
        result.put("username", normalizedEmail);
        result.put("role", registerDefaultRole);
        result.put("nickname", normalizedNickname);
        return result;
    }

    /** 邮箱验证码重置密码。 */
    public boolean resetPasswordByEmail(String email, String code, String newPassword) {
        if (!EmailCodeService.isValidEmail(email) || !isValidPassword(newPassword)) {
            return false;
        }
        String normalizedEmail = email.trim().toLowerCase();
        if (!emailCodeService.verifyCode(normalizedEmail, "reset", code)) {
            return false;
        }
        Map<String, Object> user = userDao.findByEmail(normalizedEmail);
        if (user == null) {
            return false;
        }
        Object id = user.get("id");
        long uid = id instanceof Number ? ((Number) id).longValue() : Long.parseLong(String.valueOf(id));
        userDao.updatePassword(uid, passwordEncoder.encode(newPassword));
        revokeSessions(String.valueOf(user.get("username")));
        log.info("[Auth] 邮箱密码已重置 email={}", normalizedEmail);
        return true;
    }

    /** 退出登录：删除 Redis 会话。 */
    public void logout(String authorization) {
        String token = extractToken(authorization);
        if (token != null) {
            redisTemplate.delete(TOKEN_PREFIX + token);
        }
    }

    /** 当前登录用户（从 Redis 会话还原）。 */
    public CurrentUserVO me(String authorization) {
        String token = extractToken(authorization);
        String value = token == null ? null : redisTemplate.opsForValue().get(TOKEN_PREFIX + token);
        if (value == null) {
            return null;
        }
        CurrentUserVO vo = parseSession(value);
        Map<String, Object> user = userDao.findByUsername(vo.getUsername());
        if (user != null) {
            Object nickname = user.get("nickname");
            vo.setNickname(nickname == null ? vo.getUsername() : String.valueOf(nickname));
            Object avatar = user.get("avatar");
            vo.setAvatar(avatar == null ? null : String.valueOf(avatar));
        }
        return vo;
    }

    /** 从 request 中取出已认证用户（AuthInterceptor 已解析）。 */
    public CurrentUserVO resolveCurrentUser(HttpServletRequest request) {
        Object attr = request.getAttribute("currentUser");
        return attr instanceof CurrentUserVO ? (CurrentUserVO) attr : null;
    }

    /** 重置密码后撤销该用户所有旧会话。 */
    private void revokeSessions(String username) {
        if (username == null || username.trim().isEmpty()) {
            return;
        }
        try {
            Set<String> keys = redisTemplate.keys(TOKEN_PREFIX + "*");
            if (keys == null) {
                return;
            }
            String prefix = username.trim() + SESSION_SEPARATOR;
            for (String key : keys) {
                String value = redisTemplate.opsForValue().get(key);
                if (value != null && value.startsWith(prefix)) {
                    redisTemplate.delete(key);
                }
            }
        } catch (Exception e) {
            log.warn("[Auth] 撤销旧会话失败 username={}", username, e);
        }
    }
    private LoginResultVO createSession(Map<String, Object> user, long ttlSeconds) {
        String username = String.valueOf(user.get("username"));
        String role = String.valueOf(user.get("role"));
        String token = UUID.randomUUID().toString().replace("-", "");
        redisTemplate.opsForValue().set(
                TOKEN_PREFIX + token,
                username + SESSION_SEPARATOR + role,
                Duration.ofSeconds(ttlSeconds));
        LoginResultVO vo = new LoginResultVO();
        vo.setToken(token);
        vo.setUsername(username);
        vo.setRole(role);
        Object nickname = user.get("nickname");
        vo.setNickname(nickname == null ? username : String.valueOf(nickname));
        Object avatar = user.get("avatar");
        vo.setAvatar(avatar == null ? null : String.valueOf(avatar));
        log.info("[Auth] 用户 {} 登录成功，角色 {}，ttl {}s", username, role, ttlSeconds);
        return vo;
    }

    private CurrentUserVO parseSession(String value) {
        int idx = value.lastIndexOf(SESSION_SEPARATOR);
        CurrentUserVO vo = new CurrentUserVO();
        vo.setUsername(idx > 0 ? value.substring(0, idx) : value);
        vo.setRole(idx > 0 ? value.substring(idx + 1) : "");
        return vo;
    }

    private boolean isValidPassword(String password) {
        return password != null && password.length() >= 6 && password.length() <= 64;
    }

    private boolean isEnabled(Map<String, Object> user) {
        Object status = user.get("status");
        if (status == null) {
            return true;
        }
        String s = String.valueOf(status);
        return "1".equals(s) || "true".equalsIgnoreCase(s);
    }

    private static String extractToken(String authorization) {
        if (authorization != null && authorization.startsWith("Bearer ")) {
            return authorization.substring(7);
        }
        return null;
    }
}
