package com.java3y.austin.web.service;

import com.java3y.austin.web.dao.SentinelUserDao;
import com.java3y.austin.web.vo.CurrentUserVO;
import com.java3y.austin.web.vo.LoginResultVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * AuthService 测试：bcrypt 校验、会话 token 落 Redis（username:role）、/me 还原
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AuthServiceTest {

    @Mock
    private SentinelUserDao userDao;
    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private ValueOperations<String, String> valueOps;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(redisTemplate, userDao);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
    }

    private Map<String, Object> user(String username, String rawPassword, String role) {
        Map<String, Object> user = new HashMap<>(4);
        user.put("username", username);
        user.put("password", new BCryptPasswordEncoder().encode(rawPassword));
        user.put("role", role);
        user.put("nickname", "测试" + role);
        user.put("status", 1);
        return user;
    }

    @Test
    void loginSuccess_storesUsernameRoleAndReturnsVo() {
        when(userDao.findByUsername("admin")).thenReturn(user("admin", "Admin@123", "ADMIN"));

        LoginResultVO vo = authService.login("admin", "Admin@123");

        assertNotNull(vo);
        assertEquals("admin", vo.getUsername());
        assertEquals("ADMIN", vo.getRole());
        assertNotNull(vo.getToken());
        // 会话值 = username:role，TTL 默认 7200s
        verify(valueOps).set(eq(AuthService.TOKEN_PREFIX + vo.getToken()), eq("admin:ADMIN"), any(Duration.class));
    }

    @Test
    void loginWrongPassword_returnsNull() {
        when(userDao.findByUsername("admin")).thenReturn(user("admin", "Admin@123", "ADMIN"));
        assertNull(authService.login("admin", "wrong-password"));
    }

    @Test
    void loginUserNotFound_returnsNull() {
        when(userDao.findByUsername("nobody")).thenReturn(null);
        assertNull(authService.login("nobody", "anything"));
    }

    @Test
    void loginDisabledUser_returnsNull() {
        Map<String, Object> user = user("cs", "Cs@123", "CUSTOMER_SERVICE");
        user.put("status", 0);
        when(userDao.findByUsername("cs")).thenReturn(user);
        assertNull(authService.login("cs", "Cs@123"));
    }

    @Test
    void me_parsesUsernameRoleFromSession() {
        when(valueOps.get(AuthService.TOKEN_PREFIX + "token-abc")).thenReturn("operator:OPERATOR");

        CurrentUserVO me = authService.me("Bearer token-abc");

        assertNotNull(me);
        assertEquals("operator", me.getUsername());
        assertEquals("OPERATOR", me.getRole());
    }

    @Test
    void me_invalidToken_returnsNull() {
        when(valueOps.get(AuthService.TOKEN_PREFIX + "token-nope")).thenReturn(null);
        assertNull(authService.me("Bearer token-nope"));
    }

    @Test
    void me_missingAuthHeader_returnsNull() {
        assertNull(authService.me(null));
        assertNull(authService.me("no-bearer-prefix"));
    }
}
