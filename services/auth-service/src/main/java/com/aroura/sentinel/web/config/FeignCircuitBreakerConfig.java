package com.aroura.sentinel.web.config;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;

import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JCircuitBreakerFactory;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JConfigBuilder;
import org.springframework.cloud.client.circuitbreaker.Customizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * auth-service 的 Feign 熔断参数（生效于 {@code AuthMsgClient}）。
 *
 * <h3>为什么不配 fallback</h3>
 * 不配 fallback 时，失败（含熔断打开）会以 {@code NoFallbackAvailableException} 抛出，
 * {@code AuthService.deliverSms()} 的 catch 立即返回 false、回滚验证码 —— 这是诚实的失败。
 * 若配一个"返回成功"的 fallback，用户会以为验证码发出去了而实际没有。
 * **投递类接口没有诚实的"成功"可以伪造**，所以让异常照常抛出。
 *
 * <h3>熔断器的名字（排障时别找错）</h3>
 * {@code feign.circuitbreaker.group.enabled} 默认为 false，故熔断器按**方法**粒度命名：
 * 指标是 {@code resilience4j_circuitbreaker_state{name="AuthMsgClient#send(SendRequest)"}}，
 * 不是服务名 {@code msg-service}。
 *
 * <h3>TimeLimiter 必须 ≥ connect + read</h3>
 * 默认 1s 会掐断正常调用：本客户端是 connect 3s / read 5s，故取 8s。
 * 绝不能让 TimeLimiter < read-timeout。
 *
 * <h3>已知的可观测性代价</h3>
 * 超时限制通过线程池执行，Feign 的 {@code RequestInterceptor} 会跑在那个线程上，
 * 于是 {@code MDC.get(requestId)} 读到 null —— 这一跳会**丢 X-Request-Id 与 Sleuth 父 span**。
 * 该跳是辅助链路（验证码下发），可接受。
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
