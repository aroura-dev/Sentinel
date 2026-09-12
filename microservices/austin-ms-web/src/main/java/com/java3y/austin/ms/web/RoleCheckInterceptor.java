package com.java3y.austin.ms.web;

import com.java3y.austin.web.annotation.RequireRole;
import com.java3y.austin.web.vo.CurrentUserVO;

import java.util.Arrays;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 微服务共享：角色鉴权拦截器。读取 CurrentUserFilter 还原的 currentUser attribute，
 * 比对 @RequireRole（类/方法级）。与单体 RoleInterceptor 行为一致；鉴权已由网关完成，
 * 这里只做角色校验，不查 Redis/DB。登录关闭开关保留（sentinel.login.enabled）供降级。
 *
 * @author sentinel-ms
 */
public class RoleCheckInterceptor implements HandlerInterceptor {

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
        Object attr = request.getAttribute(CurrentUserFilter.CURRENT_USER_ATTR);
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
