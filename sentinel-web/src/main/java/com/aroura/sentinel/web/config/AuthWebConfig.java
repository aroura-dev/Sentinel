package com.aroura.sentinel.web.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 注册登录拦截器（全路径，放行登录/回调/文档/健康检查）+ 角色鉴权拦截器
 * <p>
 * 拦截器按注册顺序执行：先 AuthInterceptor（认证并解析 currentUser），再 RoleInterceptor（鉴权）。
 * <p>
 * 修复：原实现只挂 {@code /api/**}，导致类级映射不在 {@code /api} 下的 Controller
 * （{@code /refresh}、{@code /account/**}、{@code /messageTemplate/**}、{@code /send}、{@code /trace/**} 等）
 * 既不经认证也不经鉴权，匿名即可访问（含删除型接口）；EXCLUDE 中针对这些路径的条目也因此从未生效。
 * 现改为 {@code /**}，并保留确实必须匿名可达的入口。
 *
 * @author sentinel
 */
@Configuration
public class AuthWebConfig implements WebMvcConfigurer {

    private static final String[] EXCLUDE = {
            // 登录入口本身
            "/api/auth/login",
            "/api/auth/sms/**",
            "/api/auth/email/**",
            // 微信服务器回调：由微信侧发起，不可能携带 Authorization 头
            "/officialAccount/receipt",
            // 小程序登录凭证校验：发生在拿到 token 之前
            "/miniProgram/sync/openid",
            "/alipayMiniProgram/sync/openid",
            // 文档与健康检查。actuator 的暴露面另行用 management.endpoints.web.exposure.include 收窄
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
                .addPathPatterns("/**")
                .excludePathPatterns(EXCLUDE);
        registry.addInterceptor(roleInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(EXCLUDE);
    }
}
