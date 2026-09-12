package com.aroura.sentinel.ms.web;

import com.aroura.sentinel.web.vo.CurrentUserVO;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import org.springframework.web.filter.OncePerRequestFilter;

/**
 * 微服务共享：网关认证后注入 {@code X-User-Name / X-User-Role} 身份头，
 * 本 Filter 在业务服务入口把它们还原成 request attribute {@code currentUser}，
 * attribute key 与单体 AuthInterceptor 完全一致，故 40+ 控制器
 * {@code request.getAttribute("currentUser")} 的行级过滤代码零改动。
 * <p>
 * 无身份头（绕过网关直连）时不设 attribute → @RequireRole 端点自然 403。
 *
 * @author sentinel-ms
 */
public class CurrentUserFilter extends OncePerRequestFilter {

    public static final String CURRENT_USER_ATTR = "currentUser";
    public static final String HDR_USER_NAME = "X-User-Name";
    public static final String HDR_USER_ROLE = "X-User-Role";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String username = request.getHeader(HDR_USER_NAME);
        String role = request.getHeader(HDR_USER_ROLE);
        if (username != null && !username.isEmpty()) {
            CurrentUserVO vo = new CurrentUserVO();
            vo.setUsername(username);
            vo.setRole(role == null ? "" : role);
            request.setAttribute(CURRENT_USER_ATTR, vo);
        }
        chain.doFilter(request, response);
    }
}
