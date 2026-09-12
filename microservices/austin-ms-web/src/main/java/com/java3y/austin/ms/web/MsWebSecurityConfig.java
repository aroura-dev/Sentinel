package com.java3y.austin.ms.web;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 微服务共享装配：注册身份头 Filter（所有请求）+ RoleCheckInterceptor（/api/**）。
 * <p>
 * 各业务服务依赖 austin-ms-web 且启动类位于 com.java3y.austin 根包下即自动生效；
 * 无需网关/注册中心的业务进程（如仅 public 接口）可通过 spring.autoconfigure / component-scan
 * 排除本包，或用 sentinel.login.enabled=false 整体关闭角色拦截。
 *
 * @author sentinel-ms
 */
@Configuration
public class MsWebSecurityConfig implements WebMvcConfigurer {

    private static final String[] EXCLUDE = {
            "/api/auth/login",
            "/actuator/**",
            "/swagger-ui/**",
            "/swagger-resources/**",
            "/v2/api-docs",
            "/webjars/**",
            "/error"
    };

    @Bean
    public RoleCheckInterceptor roleCheckInterceptor() {
        return new RoleCheckInterceptor();
    }

    @Bean
    public FilterRegistrationBean<CurrentUserFilter> currentUserFilter() {
        FilterRegistrationBean<CurrentUserFilter> reg = new FilterRegistrationBean<>(new CurrentUserFilter());
        reg.addUrlPatterns("/*");
        reg.setName("currentUserFilter");
        reg.setOrder(0);
        return reg;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(roleCheckInterceptor())
                .addPathPatterns("/api/**")
                .excludePathPatterns(EXCLUDE);
    }
}
