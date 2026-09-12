package com.java3y.austin.web.task;

import com.java3y.austin.support.constans.MessageQueuePipeline;
import com.java3y.austin.web.service.sentinel.WorkOrderEventHandler;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * EventBus 分发实现（P0-2c）
 * <p>
 * 单机模式无外部 MQ：直接把事件交给处理器同步消费，仍保留幂等表去重。
 *
 * @author sentinel
 */
@Component
@ConditionalOnProperty(name = "austin.mq.pipeline", havingValue = MessageQueuePipeline.EVENT_BUS)
public class EventBusOutboxDispatcher implements OutboxDispatcher {

    private final WorkOrderEventHandler handler;

    public EventBusOutboxDispatcher(WorkOrderEventHandler handler) {
        this.handler = handler;
    }

    @Override
    public void dispatch(String payload) {
        // 返回 false 表示重复/非法（非重试场景），无需抛异常
        handler.handle(payload);
    }
}