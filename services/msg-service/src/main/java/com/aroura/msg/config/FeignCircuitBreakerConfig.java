package com.aroura.msg.config;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;

import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JCircuitBreakerFactory;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JConfigBuilder;
import org.springframework.cloud.client.circuitbreaker.Customizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * msg-service 的 Feign 熔断参数（生效于 {@code LogisticsReceiptClient}）。
 * <p>
 * 与 auth-service 的同类配置内容一致 —— 刻意复制而不是提到 {@code sentinel-ms-web}：
 * 那个共享库只依赖 feign-core，加 circuitbreaker starter 会破坏它的极简定位。
 * 本仓库的取舍一贯是"复制加密/过滤器逻辑危险，复制纯配置可接受"。
 *
 * <h3>为什么不配 fallback</h3>
 * 回执是 fire-and-forget，调用方 {@code ReceiptEventListener} 本就吞掉异常只记日志。
 * 配 fallback 只会把"回执没送到"伪装成"送到了"，让运维失去这条线索。
 *
 * <h3>熔断器的名字（排障时别找错）</h3>
 * {@code feign.circuitbreaker.group.enabled} 默认为 false，故按**方法**粒度命名：
 * 指标是 {@code resilience4j_circuitbreaker_state{name="LogisticsReceiptClient#report(ReceiptRequest)"}}，
 * 不是服务名 {@code logistics-service}。
 *
 * <h3>TimeLimiter 必须 ≥ connect + read</h3>
 * 默认 1s 会掐断正常调用：本客户端 connect 3s / read 5s，故取 8s。
 *
 * <h3>已知的可观测性代价</h3>
 * 超时限制走线程池执行 → 这一跳的 {@code MDC.get(requestId)} 为空 →
 * **丢 X-Request-Id 与 Sleuth 父 span**。回执是辅助链路，可接受。
 *
 * @author sentinel-ms
 */
@Configuration
public class FeignCircuitBreakerConfig {

    @Bean
    public Customizer<Resilience4JCircuitBreakerFactory> feignCircuitBreaker() {
        return factory -> factory.configureDefault(id -> new Resilience4JConfigBuilder(id)
                .timeLimiterConfig(TimeLimiterConfig.custom()
                        .timeoutDuration(Duration.ofSeconds(8))
                        .build())
                .circuitBreakerConfig(CircuitBreakerConfig.custom()
                        .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
                        .slidingWindowSize(10)
                        .minimumNumberOfCalls(5)
                        .failureRateThreshold(50f)
                        .waitDurationInOpenState(Duration.ofSeconds(15))
                        .permittedNumberOfCallsInHalfOpenState(3)
                        .build())
                .build());
    }
}
