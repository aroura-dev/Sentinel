package com.aroura.sentinel.web.sms;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Service
public class SmsCodeService {

    private static final Logger log = LoggerFactory.getLogger(SmsCodeService.class);
    private static final Pattern PHONE_PATTERN = Pattern.compile("^1[3-9]\\d{9}$");
    private static final SecureRandom RANDOM = new SecureRandom();

    private static final String VERIFY_LUA =
            "local value = redis.call('get', KEYS[1]); " +
            "if not value then return -1 end; " +
            "if value == ARGV[1] then " +
            "  redis.call('del', KEYS[1]); redis.call('del', KEYS[2]); return 1; " +
            "end; " +
            "local attempts = redis.call('incr', KEYS[2]); " +
            "if attempts == 1 then redis.call('expire', KEYS[2], tonumber(ARGV[3])) end; " +
            "if attempts >= tonumber(ARGV[2]) then redis.call('del', KEYS[1]) end; " +
            "return 0;";
    private static final DefaultRedisScript<Long> VERIFY_SCRIPT =
            new DefaultRedisScript<Long>(VERIFY_LUA, Long.class);

    private final StringRedisTemplate redisTemplate;
    private final SmsProperties properties;
    private final SmsSender smsSender;

    public SmsCodeService(StringRedisTemplate redisTemplate, SmsProperties properties, SmsSender smsSender) {
        this.redisTemplate = redisTemplate;
        this.properties = properties;
        this.smsSender = smsSender;
    }

    public Map<String, Object> sendCode(String phone, String scene) {
        String p = normalizePhone(phone);
        if (p == null) {
            return null;
        }
        String s = normalizeScene(scene);

        String throttleKey = key("throttle", s, p);
        Boolean acquired = redisTemplate.opsForValue().setIfAbsent(
                throttleKey, "1", Duration.ofSeconds(properties.getSendThrottleSeconds()));
        if (acquired == null || !acquired) {
            log.warn("[SMS] 发送过于频繁 phone={} scene={}", mask(p), s);
            return null;
        }

        String dailyKey = key("daily", s, p);
        Long daily = redisTemplate.opsForValue().increment(dailyKey);
        if (daily != null && daily == 1L) {
            redisTemplate.expire(dailyKey, Duration.ofHours(24));
        }
        if (daily != null && daily > properties.getDailyLimit()) {
            redisTemplate.opsForValue().decrement(dailyKey);
            redisTemplate.delete(throttleKey);
            log.warn("[SMS] 当日发送次数超限 phone={} scene={}", mask(p), s);
            return null;
        }

        String code = String.format("%06d", RANDOM.nextInt(1000000));
        boolean delivered;
        if (properties.isEnabled()) {
            delivered = smsSender.send(p, code);
        } else {
            delivered = properties.isDevReturnCode();
            if (!delivered) {
                log.error("[SMS] 真实短信未开启且不允许开发验证码，拒绝发送 phone={}", mask(p));
            }
        }
        if (!delivered) {
            redisTemplate.delete(throttleKey);
            redisTemplate.opsForValue().decrement(dailyKey);
            return null;
        }

        redisTemplate.opsForValue().set(codeKey(s, p), code, Duration.ofSeconds(properties.getCodeTtlSeconds()));
        redisTemplate.delete(attemptKey(s, p));

        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("phone", p);
        result.put("scene", s);
        result.put("expiresIn", properties.getCodeTtlSeconds());
        if (!properties.isEnabled() && properties.isDevReturnCode()) {
            result.put("devCode", code);
        }
        log.info("[SMS] 验证码发送完成 phone={} scene={} provider={}",
                mask(p), s, properties.isEnabled() ? "tencent" : "dev");
        return result;
    }

    public boolean verifyCode(String phone, String scene, String code) {
        String p = normalizePhone(phone);
        if (p == null || code == null || !code.matches("\\d{6}")) {
            return false;
        }
        String s = normalizeScene(scene);
        List<String> keys = new ArrayList<String>(2);
        keys.add(codeKey(s, p));
        keys.add(attemptKey(s, p));
        Long result = redisTemplate.execute(
                VERIFY_SCRIPT,
                keys,
                code,
                String.valueOf(properties.getMaxAttempts()),
                "600");
        return result != null && result == 1L;
    }

    public static boolean isValidPhone(String phone) {
        return phone != null && PHONE_PATTERN.matcher(phone.trim()).matches();
    }

    private static String normalizePhone(String phone) {
        if (phone == null) {
            return null;
        }
        String p = phone.trim().replace(" ", "").replace("-", "");
        return PHONE_PATTERN.matcher(p).matches() ? p : null;
    }

    private static String normalizeScene(String scene) {
        if ("register".equalsIgnoreCase(scene)) {
            return "register";
        }
        if ("reset".equalsIgnoreCase(scene)) {
            return "reset";
        }
        return "login";
    }

    private static String codeKey(String scene, String phone) {
        return key("code", scene, phone);
    }

    private static String attemptKey(String scene, String phone) {
        return key("attempt", scene, phone);
    }

    private static String key(String type, String scene, String phone) {
        return "sentinel:sms:" + type + ":" + scene + ":" + phone;
    }

    private static String mask(String phone) {
        return phone == null || phone.length() < 7 ? "***"
                : phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }
}