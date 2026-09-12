package com.aroura.sentinel.web.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 注册登录拦截器（/api/**，放行登录/文档/健康检查）+ 角色鉴权拦截器
 * <p>
 * 拦截器按注册顺序执行：先 AuthInterceptor（认证并解析 currentUser），再 RoleInterceptor（鉴权）。
 *
 * @author sentinel
 */
@Configuration
public class AuthWebConfig implements WebMvcConfigurer {

    private static final String[] EXCLUDE = {
            "/api/auth/login",
            "/actuator/**",
            "/swagger-ui/**",
            "/swagger-resources/**",
            "/v2/api-docs",
            "/webjars/**",
            "/error"
    };

    @Autowired
    private AuthInterceptor authInterceptor;
    @Autowired
    private RoleInterceptor roleInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns(EXCLUDE);
        registry.addInterceptor(roleInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns(EXCLUDE);
    }
}
