package com.aroura.sentinel.web.config;

import com.aroura.sentinel.web.annotation.RequireRole;
import com.aroura.sentinel.web.vo.CurrentUserVO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Arrays;

/**
 * 角色鉴权拦截器：读取 AuthInterceptor 解析的 currentUser，比对 @RequireRole
 * <p>
 * 必须在 AuthInterceptor 之后注册；登录关闭（sentinel.login.enabled=false）时跳过，避免测试/降级场景误拦。
 *
 * @author sentinel
 */
@Component
public class RoleInterceptor implements HandlerInterceptor {

    @Value("${sentinel.login.enabled:true}")
    private boolean enabled;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!enabled || "OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }
        HandlerMethod handlerMethod = (HandlerMethod) handler;
        RequireRole requireRole = handlerMethod.getMethodAnnotation(RequireRole.class);
        if (requireRole == null) {
            requireRole = handlerMethod.getBeanType().getAnnotation(RequireRole.class);
        }
        if (requireRole == null) {
            return true;
        }
        Object attr = request.getAttribute(AuthInterceptor.CURRENT_USER_ATTR);
        String role = (attr instanceof CurrentUserVO) ? ((CurrentUserVO) attr).getRole() : null;
        if (role == null || Arrays.asList(requireRole.value()).indexOf(role) < 0) {
            response.setStatus(403);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"status\":403,\"msg\":\"无权限访问该资源\",\"data\":null}");
            return false;
        }
        return true;
    }
}
