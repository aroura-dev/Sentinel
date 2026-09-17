package com.aroura.sentinel.web.email;

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
public class EmailCodeService {

    private static final Logger log = LoggerFactory.getLogger(EmailCodeService.class);
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
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
    private final EmailProperties properties;
    private final EmailSender emailSender;

    public EmailCodeService(StringRedisTemplate redisTemplate, EmailProperties properties,
                            EmailSender emailSender) {
        this.redisTemplate = redisTemplate;
        this.properties = properties;
        this.emailSender = emailSender;
    }

    public Map<String, Object> sendCode(String email, String scene) {
        String normalizedEmail = normalizeEmail(email);
        if (normalizedEmail == null) {
            return null;
        }
        String normalizedScene = normalizeScene(scene);

        String throttleKey = key("throttle", normalizedScene, normalizedEmail);
        Boolean acquired = redisTemplate.opsForValue().setIfAbsent(
                throttleKey, "1", Duration.ofSeconds(properties.getSendThrottleSeconds()));
        if (acquired == null || !acquired) {
            log.warn("[Email] 发送过于频繁 email={} scene={}", mask(normalizedEmail), normalizedScene);
            return null;
        }

        String dailyKey = key("daily", normalizedScene, normalizedEmail);
        Long daily = redisTemplate.opsForValue().increment(dailyKey);
        if (daily != null && daily == 1L) {
            redisTemplate.expire(dailyKey, Duration.ofHours(24));
        }
        if (daily != null && daily > properties.getDailyLimit()) {
            redisTemplate.opsForValue().decrement(dailyKey);
            redisTemplate.delete(throttleKey);
            log.warn("[Email] 当日发送次数超限 email={} scene={}", mask(normalizedEmail), normalizedScene);
            return null;
        }

        String code = String.format("%06d", RANDOM.nextInt(1000000));
        if (!emailSender.send(normalizedEmail, code)) {
            redisTemplate.delete(throttleKey);
            if (daily != null) {
                redisTemplate.opsForValue().decrement(dailyKey);
            }
            return null;
        }

        redisTemplate.opsForValue().set(codeKey(normalizedScene, normalizedEmail), code,
                Duration.ofSeconds(properties.getCodeTtlSeconds()));
        redisTemplate.delete(attemptKey(normalizedScene, normalizedEmail));

        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("email", normalizedEmail);
        result.put("scene", normalizedScene);
        result.put("expiresIn", properties.getCodeTtlSeconds());
        log.info("[Email] 验证码发送完成 email={} scene={}", mask(normalizedEmail), normalizedScene);
        return result;
    }

    public boolean verifyCode(String email, String scene, String code) {
        String normalizedEmail = normalizeEmail(email);
        if (normalizedEmail == null || code == null || !code.matches("\\d{6}")) {
            return false;
        }
        String normalizedScene = normalizeScene(scene);
        List<String> keys = new ArrayList<String>(2);
        keys.add(codeKey(normalizedScene, normalizedEmail));
        keys.add(attemptKey(normalizedScene, normalizedEmail));
        Long result = redisTemplate.execute(
                VERIFY_SCRIPT,
                keys,
                code,
                String.valueOf(properties.getMaxAttempts()),
                "600");
        return result != null && result == 1L;
    }

    public static boolean isValidEmail(String email) {
        return email != null && email.trim().length() <= 128
                && EMAIL_PATTERN.matcher(email.trim()).matches();
    }

    private static String normalizeEmail(String email) {
        if (!isValidEmail(email)) {
            return null;
        }
        return email.trim().toLowerCase();
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

    private static String codeKey(String scene, String email) {
        return key("code", scene, email);
    }

    private static String attemptKey(String scene, String email) {
        return key("attempt", scene, email);
    }

    private static String key(String type, String scene, String email) {
        return "sentinel:email:" + type + ":" + scene + ":" + email;
    }

    private static String mask(String email) {
        if (email == null) {
            return "***";
        }
        int at = email.indexOf('@');
        if (at <= 0) {
            return "***";
        }
        return email.substring(0, 1) + "***" + email.substring(at);
    }
}