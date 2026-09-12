package com.java3y.austin.web.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 角色鉴权注解：标注在 Controller 类或方法上，要求当前用户角色命中 {@link #value()} 之一
 * <p>
 * 由 {@link com.java3y.austin.web.config.RoleInterceptor} 强制执行，未命中返回 403。
 *
 * @author sentinel
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequireRole {

    String[] value();
}
