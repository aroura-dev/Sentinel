package com.aroura.sentinel.web.email;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EmailCodeServiceTest {

    @Test
    void sendsEmailAndStoresCodeInRedis() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> values = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(values);
        when(values.setIfAbsent(anyString(), eq("1"), any(Duration.class))).thenReturn(true);
        when(values.increment(anyString())).thenReturn(1L);

        EmailProperties properties = new EmailProperties();
        AtomicReference<String> deliveredCode = new AtomicReference<String>();
        EmailCodeService service = new EmailCodeService(redis, properties,
                (email, code) -> {
                    deliveredCode.set(code);
                    return true;
                });

        Map<String, Object> result = service.sendCode("User@Example.com", "login");

        assertNotNull(result);
        assertEquals("user@example.com", result.get("email"));
        assertEquals(300L, result.get("expiresIn"));
        assertTrue(deliveredCode.get().matches("\\d{6}"));
        assertNull(result.get("devCode"));
        verify(values).set(eq("sentinel:email:code:login:user@example.com"), anyString(), any(Duration.class));
    }

    @Test
    void rejectsMailWhenSenderFails() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> values = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(values);
        when(values.setIfAbsent(anyString(), eq("1"), any(Duration.class))).thenReturn(true);
        when(values.increment(anyString())).thenReturn(1L);

        EmailCodeService service = new EmailCodeService(redis, new EmailProperties(),
                (email, code) -> false);

        assertNull(service.sendCode("user@example.com", "login"));
    }

    @Test
    void validatesEmailFormat() {
        assertTrue(EmailCodeService.isValidEmail("user@example.com"));
        assertTrue(!EmailCodeService.isValidEmail("not-an-email"));
    }
}