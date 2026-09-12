package com.java3y.austin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * sentinel-ms 认证服务入口（独立进程 :8081）。
 * 扫描根包 com.java3y.austin：web.controller.auth + web.service + web.dao + austin-ms-web 共享装配。
 * 数据库：sentinel_auth（sentinel_user/role/role_menu）；Redis：会话 token。
 *
 * @author sentinel-ms
 */
@SpringBootApplication
public class AuthApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuthApplication.class, args);
    }
}
