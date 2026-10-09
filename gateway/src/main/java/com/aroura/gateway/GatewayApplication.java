package com.aroura.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * sentinel-ms 统一网关入口（:8080）。
 * 认证（读 Redis 会话）→ 注入 X-User-Name/X-User-Role → 路由到 auth/logistics 等业务服务。
 *
 * @author sentinel-ms
 */
@SpringBootApplication
public class GatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }
}
