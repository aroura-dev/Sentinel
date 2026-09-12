package com.java3y.logistics;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;

/**
 * sentinel-ms 履约物流服务入口（独立进程 :8083，连 sentinel_logistics）。
 * <p>
 * 复用 sentinel-logistics 领域模块（订单/轨迹/通知 DAO 等），启动类置于 com.java3y.logistics：
 * - 默认扫描 com.java3y.logistics（本服务装配）；
 * - 显式扫描 com.java3y.austin 装载 sentinel-logistics 的 @Repository/@Service；
 *   但剔除该模块自带的定时扫描任务与 @EnableScheduling 配置（切分后由本进程按需控制）。
 * 通知闭环的 AI 文案与最终触达改为调用 agent-service(:8084)/msg-service(:8082) 的 REST。
 *
 * @author sentinel-ms
 */
@SpringBootApplication
@ComponentScan(basePackages = {"com.java3y.austin", "com.java3y.logistics"}, excludeFilters = {
        @ComponentScan.Filter(type = FilterType.REGEX, pattern =
                "com\\.java3y\\.austin\\.logistics\\.config\\..*" +
                        "|com\\.java3y\\.austin\\.logistics\\.task\\..*")
})
public class LogisticsApplication {

    public static void main(String[] args) {
        SpringApplication.run(LogisticsApplication.class, args);
    }
}
