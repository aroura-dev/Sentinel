package com.aroura.sentinel.web.service;

import com.aroura.sentinel.web.dao.SentinelUserDao;
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
import java.util.Map;
import java.util.UUID;

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

    private final StringRedisTemplate redisTemplate;
    private final SentinelUserDao userDao;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Value("${sentinel.login.token-ttl-seconds:7200}")
    private long tokenTtlSeconds;

    public AuthService(StringRedisTemplate redisTemplate, SentinelUserDao userDao) {
        this.redisTemplate = redisTemplate;
        this.userDao = userDao;
    }

    /**
     * 登录：bcrypt 校验密码，成功则签发 Redis 会话 token
     *
     * @return 登录结果；用户名/密码错误或账号停用时返回 null
     */
    public LoginResultVO login(String username, String password) {
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
        String role = String.valueOf(user.get("role"));
        String token = UUID.randomUUID().toString().replace("-", "");
        redisTemplate.opsForValue().set(
                TOKEN_PREFIX + token,
                username.trim() + SESSION_SEPARATOR + role,
                Duration.ofSeconds(tokenTtlSeconds));

        LoginResultVO vo = new LoginResultVO();
        vo.setToken(token);
        vo.setUsername(username.trim());
        vo.setRole(role);
        Object nickname = user.get("nickname");
        vo.setNickname(nickname == null ? username.trim() : String.valueOf(nickname));
        log.info("[Auth] 用户 {} 登录成功，角色 {}", vo.getUsername(), role);
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
