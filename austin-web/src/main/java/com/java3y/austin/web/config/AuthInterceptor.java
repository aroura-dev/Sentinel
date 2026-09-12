package com.java3y.austin.web.config;

import com.java3y.austin.web.vo.CurrentUserVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 登录拦截器：/api/** 需要有效 token（Redis 校验），白名单放行
 * <p>
 * 校验通过后把解析出的 {@link CurrentUserVO}（username:role）放入 request attribute，
 * 供 {@link RoleInterceptor} 做角色鉴权。
 *
 * @author sentinel
 */
@Component
public class AuthInterceptor implements HandlerInterceptor {

    private static final String TOKEN_PREFIX = "sentinel:token:";
    private static final String SESSION_SEPARATOR = ":";
    public static final String CURRENT_USER_ATTR = "currentUser";

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Value("${sentinel.login.enabled:true}")
    private boolean enabled;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!enabled || "OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String authorization = request.getHeader("Authorization");
        String token = null;
        if (authorization != null && authorization.startsWith("Bearer ")) {
            token = authorization.substring(7);
        }
        if (token != null) {
            String value = redisTemplate.opsForValue().get(TOKEN_PREFIX + token);
            if (value != null) {
                request.setAttribute(CURRENT_USER_ATTR, parseSession(value));
                return true;
            }
        }
        response.setStatus(401);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"status\":401,\"msg\":\"未登录或登录已过期\",\"data\":null}");
        return false;
    }

    private static CurrentUserVO parseSession(String value) {
        int idx = value.lastIndexOf(SESSION_SEPARATOR);
        CurrentUserVO vo = new CurrentUserVO();
        vo.setUsername(idx > 0 ? value.substring(0, idx) : value);
        vo.setRole(idx > 0 ? value.substring(idx + 1) : "");
        return vo;
    }
}
