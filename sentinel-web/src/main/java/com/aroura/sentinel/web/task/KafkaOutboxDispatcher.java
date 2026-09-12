package com.aroura.sentinel.web.task;

import com.aroura.sentinel.support.constans.MessageQueuePipeline;
import com.aroura.sentinel.support.mq.SendMqService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Kafka 分发实现（P0-2c）
 *
 * @author sentinel
 */
@Component
@ConditionalOnProperty(name = "sentinel.mq.pipeline", havingValue = MessageQueuePipeline.KAFKA)
public class KafkaOutboxDispatcher implements OutboxDispatcher {

    private final SendMqService sendMqService;

    @Value("${sentinel.outbox.topic:sentinel_outbox}")
    private String topic;

    public KafkaOutboxDispatcher(SendMqService sendMqService) {
        this.sendMqService = sendMqService;
    }

    @Override
    public void dispatch(String payload) {
        sendMqService.send(topic, payload);
    }
}