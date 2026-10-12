package com.aroura.sentinel;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * sentinel-ms 认证服务入口（独立进程 :8081）。
 * 扫描根包 com.aroura.sentinel：web.controller.auth + web.service + web.dao + sentinel-ms-web 共享装配。
 * 数据库：sentinel_auth（sentinel_user/role/role_menu）；Redis：会话 token。
 *
 * @author sentinel-ms
 */
// Feign 客户端（验证码短信下发 msg-service）。只扫本服务的 client 包。
@EnableFeignClients(basePackages = "com.aroura.sentinel.web.client")
@SpringBootApplication
public class AuthApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuthApplication.class, args);
    }
}
