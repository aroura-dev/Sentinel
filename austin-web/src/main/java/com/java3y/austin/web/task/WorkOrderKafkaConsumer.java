package com.java3y.austin.web.task;

import com.java3y.austin.web.service.sentinel.WorkOrderEventHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * 工单 outbox 事件的 Kafka 消费者适配器（P0-2b）
 * <p>
 * 仅在 austin.mq.pipeline=kafka 时启用；幂等与业务处理在 {@link WorkOrderEventHandler}。
 *
 * @author sentinel
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "austin.mq.pipeline", havingValue = "kafka")
public class WorkOrderKafkaConsumer {

    private final WorkOrderEventHandler handler;

    public WorkOrderKafkaConsumer(WorkOrderEventHandler handler) {
        this.handler = handler;
    }

    @KafkaListener(
            topics = "${sentinel.outbox.topic:sentinel_outbox}",
            groupId = "sentinel-workorder-outbox",
            containerFactory = "filterContainerFactory")
    public void onMessage(String message) {
        try {
            handler.handle(message);
        } catch (Exception e) {
            log.warn("[Outbox] 消费异常，将由 MQ 重试: {}", e.getMessage());
            throw e;
        }
    }
}