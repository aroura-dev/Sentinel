package com.java3y.austin.web.service;

import com.java3y.austin.web.dao.SentinelUserDao;
import com.java3y.austin.web.vo.CurrentUserVO;
import com.java3y.austin.web.vo.LoginResultVO;
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
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.ContentType;
import com.alibaba.fastjson2.JSON;

/**
 * 多角色登录认证服务
 * <p>
 * 认证体系：sentinel_user 表（bcrypt 密码 + 角色）+ Redis 会话 token。
 * Redis 会话值存 {@code username:role}，AuthInterceptor 每次请求解析后放入
 * {@code request} attribute，供 {@code RoleInterceptor} 做角色鉴权（无 DB 命中、一次 Redis 读）。
 * 局限（文档化）：改角色需重新登录。
 *
 * @author sentinel
 */
@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    public static final String TOKEN_PREFIX = "sentinel:token:";
    public static final String SESSION_SEPARATOR = ":";
    /** 短信验证码 Redis key 前缀 */
    public static final String SMS_CODE_PREFIX = "sentinel:sms:";

    private final StringRedisTemplate redisTemplate;
    private final SentinelUserDao userDao;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Value("${sentinel.login.token-ttl-seconds:7200}")
    private long tokenTtlSeconds;
    /** 记住我：长效会话 TTL（默认 7 天） */
    @Value("${auth.remember-ttl-seconds:604800}")
    private long rememberTtlSeconds;
    /** 手机号自助注册默认角色 */
    @Value("${auth.register-default-role:MERCHANT}")
    private String registerDefaultRole;
    @Value("${sms.code.ttl-seconds:300}")
    private long smsCodeTtlSeconds;
    /** 真实环境不回传验证码；仅演示/联调可置 true */
    @Value("${sms.dev-return-code:false}")
    private boolean devReturnCode;
    /** 真实短信引擎：true 时验证码经 msg-service 短信模板下发 */
    @Value("${sms.engine.enabled:true}")
    private boolean engineEnabled;
    @Value("${sms.engine.msg-url:http://127.0.0.1:8082}")
    private String engineMsgUrl;
    @Value("${sms.engine.template-id:1011}")
    private long engineTemplateId;
    /** 单手机号 60s 内限发 1 条、单次验证码最多尝试次数（防爆破） */
    @Value("${sms.send-throttle-seconds:60}")
    private long sendThrottleSeconds;
    @Value("${sms.max-attempts:5}")
    private int maxAttempts;

    public AuthService(StringRedisTemplate redisTemplate, SentinelUserDao userDao) {
        this.redisTemplate = redisTemplate;
        this.userDao = userDao;
    }

    /**
     * 登录：bcrypt 校验密码，成功则签发 Redis 会话 token
     *
     * @param remember 记住我：true 时签发长效 token（auth.remember-ttl-seconds）
     * @return 登录结果；用户名/密码错误或账号停用时返回 null
     */
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

    /**
     * 手机验证码：发送
     *
     * @param scene login=须已注册启用（登录/重置用）；register=须未注册（注册用）
     * @return 发送结果；校验不通过或发送受限返回 null
     */
    public Map<String, Object> sendSmsCode(String phone, String scene) {
        String p = normalizePhone(phone);
        if (p == null) {
            return null;
        }
        boolean register = "register".equalsIgnoreCase(scene);
        Map<String, Object> user = userDao.findByPhone(p);
        if (register) {
            if (user != null) {
                return null; // 已注册：不可再以注册场景发码
            }
        } else if (user == null || !isEnabled(user)) {
            return null;     // 登录/重置场景：须已注册且启用
        }
        // 60s 限发
        String throttleKey = "sentinel:sms:throttle:" + p;
        Boolean ok = redisTemplate.opsForValue().setIfAbsent(throttleKey, "1", Duration.ofSeconds(sendThrottleSeconds));
        if (ok == null || !ok) {
            return null;
        }

        String code = String.format("%06d", ThreadLocalRandom.current().nextInt(1000000));
        String codeKey = SMS_CODE_PREFIX + p;
        // 真实引擎：经 msg-service 短信模板下发；失败则回滚验证码
        if (engineEnabled && engineTemplateId > 0) {
            if (!deliverSms(p, code)) {
                redisTemplate.delete(codeKey);
                redisTemplate.delete(throttleKey);
                log.warn("[Auth] 短信下发失败 phone={}", p);
                return null;
            }
        }
        redisTemplate.opsForValue().set(codeKey, code, Duration.ofSeconds(smsCodeTtlSeconds));
        redisTemplate.delete("sentinel:sms:attempt:" + p);
        log.info("[Auth] 验证码已下发 phone={} scene={}", p, register ? "register" : "login");

        Map<String, Object> resp = new HashMap<>(4);
        resp.put("phone", p);
        if (devReturnCode) {
            resp.put("devCode", code);
        }
        return resp;
    }

    /** 经 msg-service /send 走真实短信引擎（模板 sentinel:sms-code） */
    private boolean deliverSms(String phone, String code) {
        try {
            Map<String, Object> messageParam = new HashMap<>(4);
            messageParam.put("bizId", "smscode-" + phone);
            messageParam.put("receiver", phone);
            Map<String, Object> vars = new HashMap<>(2);
            vars.put("code", code);
            messageParam.put("variables", vars);
            Map<String, Object> req = new HashMap<>(4);
            req.put("code", "send");
            req.put("messageTemplateId", engineTemplateId);
            req.put("messageParam", messageParam);
            String body = HttpRequest.post(engineMsgUrl + "/send")
                    .header("Content-Type", ContentType.JSON.getValue())
                    .body(JSON.toJSONString(req))
                    .timeout(5000)
                    .execute().body();
            if (body == null) {
                return false;
            }
            return "0".equals(JSON.parseObject(body).getString("code"));
        } catch (Exception e) {
            log.warn("[Auth] deliverSms err {}", e.toString());
            return false;
        }
    }

    /**
     * 手机验证码登录：校验验证码（一次性）成功后签发会话
     *
     * @param remember 记住我：true 时签发长效 token
     * @return 登录结果；手机号未注册 / 验证码错误或过期返回 null
     */
    public LoginResultVO loginByCode(String phone, String code, boolean remember) {
        if (phone == null || code == null || phone.trim().isEmpty() || code.trim().isEmpty()) {
            return null;
        }
        String p = phone.trim();
        if (!consumeCodeIfMatch(p, code.trim())) {
            return null;
        }
        Map<String, Object> user = userDao.findByPhone(p);
        if (user == null || !isEnabled(user)) {
            return null;
        }
        return createSession(user, remember ? rememberTtlSeconds : tokenTtlSeconds);
    }

    /**
     * 验证码校验（一次性）：防爆破（超过 maxAttempts 锁并清码）、成功即消费删除
     */
    private boolean consumeCodeIfMatch(String phone, String code) {
        String key = SMS_CODE_PREFIX + phone;
        String attemptKey = "sentinel:sms:attempt:" + phone;
        Object att = redisTemplate.opsForValue().get(attemptKey);
        if (att != null && Integer.parseInt(String.valueOf(att)) >= maxAttempts) {
            redisTemplate.delete(key);
            return false;
        }
        Object saved = redisTemplate.opsForValue().get(key);
        if (saved == null || !String.valueOf(saved).equalsIgnoreCase(code)) {
            Long n = redisTemplate.opsForValue().increment(attemptKey);
            if (n != null && n == 1L) {
                redisTemplate.expire(attemptKey, Duration.ofMinutes(10));
            }
            return false;
        }
        redisTemplate.delete(key);       // 一次性
        redisTemplate.delete(attemptKey);
        return true;
    }

    /**
     * 注册：验证码须为注册场景下发，成功后建号（username=phone，默认角色 register-default-role）
     *
     * @return 注册成功返回 {username, role, nickname}；手机号已注册/验证码错误/密码不合法返回 null
     */
    public Map<String, Object> register(String phone, String code, String password, String nickname) {
        String p = normalizePhone(phone);
        if (p == null || !isValidPassword(password)) {
            return null;
        }
        if (!consumeCodeIfMatch(p, code == null ? "" : code.trim())) {
            return null;
        }
        if (userDao.findByPhone(p) != null) {
            return null;
        }
        String nick = (nickname == null || nickname.trim().isEmpty()) ? p : nickname.trim();
        Long id = userDao.insert(p, p, passwordEncoder.encode(password), nick, registerDefaultRole, "1");
        if (id == null) {
            return null;
        }
        Map<String, Object> out = new HashMap<>(3);
        out.put("username", p);
        out.put("role", registerDefaultRole);
        out.put("nickname", nick);
        log.info("[Auth] 注册成功 username={} role={}", p, registerDefaultRole);
        return out;
    }

    /**
     * 重置密码：验证码为登录场景下发（须手机号已注册），通过后更新 bcrypt 密码
     */
    public boolean resetPassword(String phone, String code, String newPassword) {
        String p = normalizePhone(phone);
        if (p == null || !isValidPassword(newPassword)) {
            return false;
        }
        if (!consumeCodeIfMatch(p, code == null ? "" : code.trim())) {
            return false;
        }
        Map<String, Object> user = userDao.findByPhone(p);
        if (user == null) {
            return false;
        }
        Object id = user.get("id");
        long uid = id instanceof Number ? ((Number) id).longValue() : Long.parseLong(String.valueOf(id));
        userDao.updatePassword(uid, passwordEncoder.encode(newPassword));
        log.info("[Auth] 密码已重置 phone={}", p);
        return true;
    }

    private String normalizePhone(String phone) {
        if (phone == null || phone.trim().isEmpty()) {
            return null;
        }
        return phone.trim();
    }

    private boolean isValidPassword(String password) {
        return password != null && password.length() >= 6 && password.length() <= 64;
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
        log.info("[Auth] 用户 {} 登录成功，角色 {}，ttl {}s", username, role, ttlSeconds);
        return vo;
    }

    /**
     * 退出登录：删除 Redis 会话
     */
    public void logout(String authorization) {
        String token = extractToken(authorization);
        if (token != null) {
            redisTemplate.delete(TOKEN_PREFIX + token);
        }
    }

    /**
     * 当前登录用户（从 Redis 会话还原）
     *
     * @return 未登录或会话过期返回 null
     */
    public CurrentUserVO me(String authorization) {
        String token = extractToken(authorization);
        String value = token == null ? null : redisTemplate.opsForValue().get(TOKEN_PREFIX + token);
        return value == null ? null : parseSession(value);
    }

    /**
     * 从 request 中取出已认证用户（AuthInterceptor 已解析）
     */
    public CurrentUserVO resolveCurrentUser(HttpServletRequest request) {
        Object attr = request.getAttribute("currentUser");
        return attr instanceof CurrentUserVO ? (CurrentUserVO) attr : null;
    }

    private CurrentUserVO parseSession(String value) {
        int idx = value.lastIndexOf(SESSION_SEPARATOR);
        CurrentUserVO vo = new CurrentUserVO();
        vo.setUsername(idx > 0 ? value.substring(0, idx) : value);
        vo.setRole(idx > 0 ? value.substring(idx + 1) : "");
        return vo;
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
