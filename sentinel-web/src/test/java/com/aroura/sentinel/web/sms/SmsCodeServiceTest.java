package com.aroura.sentinel.web.sms;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SmsCodeServiceTest {

    @Test
    void sendsDevCodeAndStoresItInRedis() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> values = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(values);
        when(values.setIfAbsent(anyString(), eq("1"), any(Duration.class))).thenReturn(true);
        when(values.increment(anyString())).thenReturn(1L);

        SmsProperties properties = new SmsProperties();
        properties.setEnabled(false);
        properties.setDevReturnCode(true);
        SmsCodeService service = new SmsCodeService(redis, properties, (phone, code) -> null);

        Map<String, Object> result = service.sendCode("13800138000", "login");

        assertNotNull(result);
        assertEquals("13800138000", result.get("phone"));
        assertEquals(300L, result.get("expiresIn"));
        assertTrue(String.valueOf(result.get("devCode")).matches("\\d{6}"));
        verify(values).set(eq("sentinel:sms:code:login:13800138000"), anyString(), any(Duration.class));
    }

    @Test
    void validatesPhoneFormat() {
        assertTrue(SmsCodeService.isValidPhone("13800138000"));
    }
}