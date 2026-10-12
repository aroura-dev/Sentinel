package com.aroura.gateway.config;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;

import org.springframework.cloud.circuitbreaker.resilience4j.ReactiveResilience4JCircuitBreakerFactory;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JConfigBuilder;
import org.springframework.cloud.client.circuitbreaker.Customizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * 网关路由级熔断参数。
 *
 * <h3>为什么参数写在 Java 而不是 application.yml</h3>
 * Resilience4j 的 {@code TimeLimiter} 默认超时**只有 1 秒**，而它只能通过
 * {@link TimeLimiterConfig} 关闭或放宽 —— YAML 配不到。不覆盖它的后果是：
 * 下游正常但耗时 >1s 的请求会被直接判超时，用户看到 504，而下游其实好好的。
 *
 * <h3>为什么是 20s 而不是各路由分开</h3>
 * {@code configureDefault} 只能设一个默认值。logistics 路由内部是**串行两跳**
 * （agent 生成文案 read-timeout 8s → msg 投递 read-timeout 8s，见
 * {@code LogisticsNotifyService.send()}），最坏 16s+，给 20s 才不会误杀主链路。
 * 代价是 auth 真挂时最坏多等 —— 但误杀物流主链路比这严重得多。
 *
 * <h3>计数语义（决定了这套熔断只在什么时候开闸）</h3>
 * 业务失败时下游仍返回 HTTP 200（本项目"200 + 业务码"的约定，例如 msg 拒绝受理），
 * 所以断路器**只在连接失败/超时时计数**。也就是说：它只在服务真的宕机或假死时打开，
 * 业务层的失败由各服务自己的状态机负责 —— 两者互不干扰。
 *
 * @author sentinel-ms
 */
@Configuration
public class GatewayCircuitBreakerConfig {

    @Bean
    public Customizer<ReactiveResilience4JCircuitBreakerFactory> gatewayCircuitBreaker() {
        return factory -> factory.configureDefault(id -> new Resilience4JConfigBuilder(id)
                // 见类注释：默认 1s 必须覆盖，否则误杀正常慢请求
                .timeLimiterConfig(TimeLimiterConfig.custom()
                        .timeoutDuration(Duration.ofSeconds(20))
                        .build())
                .circuitBreakerConfig(CircuitBreakerConfig.custom()
                        // 演示栈 QPS 低：COUNT_BASED 比 TIME_BASED 更快积累出有效窗口
                        .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
                        .slidingWindowSize(20)
                        // 防止"一次抖动就开闸"：至少观察到 5 次调用才开始算失败率
                        .minimumNumberOfCalls(5)
                        .failureRateThreshold(50f)
                        // open 取 10s：故障恢复后要能较快重新探测，不宜久挂
                        .waitDurationInOpenState(Duration.ofSeconds(10))
                        .permittedNumberOfCallsInHalfOpenState(3)
                        .build())
                .build());
    }
}
