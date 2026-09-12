package com.aroura.sentinel.web.config;

import com.aroura.sentinel.common.vo.BasicResultVO;
import com.aroura.sentinel.web.annotation.RequireRole;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 角色鉴权拦截器测试：standalone MockMvc（不启 Spring 容器），验证 @RequireRole 403/放行
 */
@ExtendWith(MockitoExtension.class)
class RoleInterceptorTest {

    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private ValueOperations<String, String> valueOps;

    private MockMvc mockMvc(String sessionValue) {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get("sentinel:token:tok")).thenReturn(sessionValue);

        AuthInterceptor auth = new AuthInterceptor();
        ReflectionTestUtils.setField(auth, "redisTemplate", redisTemplate);
        ReflectionTestUtils.setField(auth, "enabled", true);

        RoleInterceptor role = new RoleInterceptor();
        ReflectionTestUtils.setField(role, "enabled", true);

        return MockMvcBuilders.standaloneSetup(new ProtectedController())
                .addInterceptors(auth, role)
                .build();
    }

    @Test
    void adminToken_canAccessProtectedEndpoint() throws Exception {
        mockMvc("admin:ADMIN")
                .perform(get("/api/protected").header("Authorization", "Bearer tok"))
                .andExpect(status().isOk());
    }

    @Test
    void merchantToken_forbiddenOnAdminOnlyEndpoint() throws Exception {
        mockMvc("merchant:MERCHANT")
                .perform(get("/api/protected").header("Authorization", "Bearer tok"))
                .andExpect(status().isForbidden());
    }

    @Test
    void missingToken_unauthorized() throws Exception {
        mockMvc(null)
                .perform(get("/api/protected").header("Authorization", "Bearer tok"))
                .andExpect(status().isUnauthorized());
    }

    @RestController
    static class ProtectedController {

        @RequireRole({"ADMIN"})
        @GetMapping("/api/protected")
        public BasicResultVO protectedEndpoint() {
            return BasicResultVO.success("ok");
        }
    }
}
