package com.aroura.msg;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * sentinel-ms 消息触达服务入口（独立进程 :8082，连 sentinel_msg + Kafka :19092）。
 * <p>
 * 启动类置于 com.aroura.msg（与被扫业务包不重叠）：
 * - 默认扫描 com.aroura.msg（本应用装配 MsPipelineConfig 等）；
 * - 显式扫描 com.aroura.sentinel 复用 sentinel 引擎，但按类名剔除 handler 内嵌的
 *   3 个 Agent 调用 Action 与原始 TaskPipelineConfig（其引用 sentinel-agent/logistics，
 *   不在本服务 classpath）。剔除用 REGEX 匹配类名，避免加载这些类的字节码。
 * 本服务管道为：Discard→Shield→Dedup→SensWords→SendMessage（见 MsPipelineConfig）。
 *
 * @author sentinel-ms
 */
@SpringBootApplication
@ComponentScan(basePackages = {"com.aroura.sentinel", "com.aroura.msg"}, excludeFilters = {
        @ComponentScan.Filter(type = FilterType.REGEX, pattern =
                "com\\.aroura\\.sentinel\\.handler\\.action\\.Agent\\w*Action" +
                        "|com\\.aroura\\.sentinel\\.handler\\.config\\.TaskPipelineConfig")
})
// 启动类在 com.aroura.msg，默认 JPA 扫描根不覆盖 sentinel 库，显式指定其仓库/实体包
@EntityScan(basePackages = "com.aroura.sentinel.support.domain")
@EnableJpaRepositories(basePackages = "com.aroura.sentinel.support.dao")
public class MsgApplication {

    public static void main(String[] args) {
        SpringApplication.run(MsgApplication.class, args);
    }
}
