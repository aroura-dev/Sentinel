package com.aroura.sentinel.ms.web;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 微服务共享装配：注册身份头 Filter（所有请求）+ RoleCheckInterceptor（/api/**）。
 * <p>
 * 各业务服务依赖 sentinel-ms-web 且启动类位于 com.aroura.sentinel 根包下即自动生效；
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

    /** 与网关共享的身份签名密钥；为空则不校验（仅限本地无网关联调）。 */
    @Value("${sentinel.internal.secret:}")
    private String internalSecret;
    /** 签名时间戳允许的偏移秒数，用于限制重放窗口。 */
    @Value("${sentinel.internal.max-skew-seconds:300}")
    private long internalMaxSkewSeconds;

    /**
     * 请求关联 ID 必须排在身份过滤器之前：后者在签名校验失败时会写 WARN 日志，
     * 那条日志同样需要带上 requestId 才能和调用方的请求对上。
     */
    @Bean
    public FilterRegistrationBean<RequestIdFilter> requestIdFilter() {
        FilterRegistrationBean<RequestIdFilter> reg = new FilterRegistrationBean<>(new RequestIdFilter());
        reg.addUrlPatterns("/*");
        reg.setName("requestIdFilter");
        reg.setOrder(-1);
        return reg;
    }

    @Bean
    public FilterRegistrationBean<CurrentUserFilter> currentUserFilter() {
        FilterRegistrationBean<CurrentUserFilter> reg =
                new FilterRegistrationBean<>(new CurrentUserFilter(internalSecret, internalMaxSkewSeconds));
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
