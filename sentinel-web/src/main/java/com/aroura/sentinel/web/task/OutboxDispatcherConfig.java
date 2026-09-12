package com.aroura.sentinel.web.task;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Outbox 分发器兜底配置（P0-2c）
 * <p>
 * 未匹配到 KAFKA/EVENT_BUS 分发器时（如 redis/rocketMq 等未接入场景），
 * 记录告警并按已投递处理，避免事件无限重试。
 *
 * @author sentinel
 */
@Slf4j
@Configuration
public class OutboxDispatcherConfig {

    @Bean
    @ConditionalOnMissingBean(OutboxDispatcher.class)
    public OutboxDispatcher loggingOutboxDispatcher() {
        return payload -> log.warn("[Outbox] 未匹配到分发器，事件被忽略（请检查 sentinel.mq.pipeline）: {}", payload);
    }
}