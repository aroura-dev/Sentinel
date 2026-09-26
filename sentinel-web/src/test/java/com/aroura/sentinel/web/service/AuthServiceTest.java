package com.aroura.sentinel.web.service;

import com.aroura.sentinel.web.dao.SentinelUserDao;
import com.aroura.sentinel.web.email.EmailCodeService;
import com.aroura.sentinel.web.service.sentinel.tms.MerchantService;
import com.aroura.sentinel.web.sms.SmsCodeService;
import com.aroura.sentinel.web.vo.CurrentUserVO;
import com.aroura.sentinel.web.vo.LoginResultVO;
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
import org.springframework.test.util.ReflectionTestUtils;

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
 * AuthService 测试：bcrypt 校验、会话 token 落 Redis（v2:userId:merchantId:username:role）、/me 还原。
 * <p>
 * {@link #me_parsesUsernameRoleFromSession()} 刻意使用 **legacy 格式** 会话串，
 * 作为 {@link com.aroura.sentinel.web.config.SessionCodec} 旧格式兼容分支的回归网。
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
    @Mock
    private SmsCodeService smsCodeService;
    @Mock
    private EmailCodeService emailCodeService;
    @Mock
    private MerchantService merchantService;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(redisTemplate, userDao, smsCodeService, emailCodeService, merchantService);
        // @Value 字段在无 Spring 上下文的单测里是 null，显式补上生产默认值
        ReflectionTestUtils.setField(authService, "registerDefaultRole", "MERCHANT");
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
    }

    private Map<String, Object> user(String username, String rawPassword, String role) {
        Map<String, Object> user = new HashMap<String, Object>(5);
        user.put("id", 1L);
        user.put("username", username);
        user.put("password", new BCryptPasswordEncoder().encode(rawPassword));
        user.put("role", role);
        user.put("nickname", "测试" + role);
        user.put("status", 1);
        return user;
    }

    @Test
    void loginSuccess_storesUsernameRoleAndReturnsVo() {
        when(userDao.findByUsername("张伟")).thenReturn(user("张伟", "Admin@123", "ADMIN"));

        LoginResultVO vo = authService.login("张伟", "Admin@123");

        assertNotNull(vo);
        assertEquals("张伟", vo.getUsername());
        assertEquals("ADMIN", vo.getRole());
        assertNotNull(vo.getToken());
        // 未绑定商家（merchantService 未打桩返回 null）→ merchantId 段写 0
        verify(valueOps).set(eq(AuthService.TOKEN_PREFIX + vo.getToken()),
                eq("v2:1:0:张伟:ADMIN"), any(Duration.class));
    }

    @Test
    void loginSuccess_boundMerchantIsCarriedInSession() {
        when(userDao.findByUsername("陈浩")).thenReturn(user("陈浩", "Merchant@123", "MERCHANT"));
        when(merchantService.findMerchantIdByUserId(1L)).thenReturn(7L);

        LoginResultVO vo = authService.login("陈浩", "Merchant@123");

        assertNotNull(vo);
        assertEquals(Long.valueOf(7L), vo.getMerchantId());
        verify(valueOps).set(eq(AuthService.TOKEN_PREFIX + vo.getToken()),
                eq("v2:1:7:陈浩:MERCHANT"), any(Duration.class));
    }

    @Test
    void registerByPhone_provisionsMerchantForNewUser() {
        when(userDao.findByPhone("13800000000")).thenReturn(null);
        when(userDao.existsByUsername("新商家")).thenReturn(false);
        when(smsCodeService.verifyCode("13800000000", "register", "123456")).thenReturn(true);
        when(userDao.insert(eq("新商家"), eq("13800000000"), any(), eq("新商家"), eq("MERCHANT"), eq("1")))
                .thenReturn(42L);

        Map<String, Object> result = authService.registerByPhone("13800000000", "123456", "新商家", "pass123456", null);

        assertNotNull(result);
        // 自助注册默认角色是 MERCHANT，必须同时建商家档案，否则该账号在租户隔离下看不到任何数据
        verify(merchantService).createForNewUser(42L, "新商家");
    }

    @Test
    void registerByPhone_merchantProvisionFailureDoesNotFailRegistration() {
        when(userDao.findByPhone("13800000001")).thenReturn(null);
        when(userDao.existsByUsername("新商家2")).thenReturn(false);
        when(smsCodeService.verifyCode("13800000001", "register", "123456")).thenReturn(true);
        when(userDao.insert(any(), any(), any(), any(), any(), any())).thenReturn(43L);
        when(merchantService.createForNewUser(43L, "新商家2")).thenThrow(new RuntimeException("db down"));

        // 建档案失败不应让注册整体失败
        assertNotNull(authService.registerByPhone("13800000001", "123456", "新商家2", "pass123456", null));
    }

    @Test
    void loginWrongPassword_returnsNull() {
        when(userDao.findByUsername("张伟")).thenReturn(user("张伟", "Admin@123", "ADMIN"));
        assertNull(authService.login("张伟", "wrong-password"));
    }

    @Test
    void loginUserNotFound_returnsNull() {
        when(userDao.findByUsername("nobody")).thenReturn(null);
        assertNull(authService.login("nobody", "anything"));
    }

    @Test
    void loginDisabledUser_returnsNull() {
        Map<String, Object> user = user("王芳", "Cs@123", "CUSTOMER_SERVICE");
        user.put("status", 0);
        when(userDao.findByUsername("王芳")).thenReturn(user);
        assertNull(authService.login("王芳", "Cs@123"));
    }

    @Test
    void me_parsesUsernameRoleFromSession() {
        when(valueOps.get(AuthService.TOKEN_PREFIX + "token-abc")).thenReturn("刘洋:OPERATOR");

        CurrentUserVO me = authService.me("Bearer token-abc");

        assertNotNull(me);
        assertEquals("刘洋", me.getUsername());
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
