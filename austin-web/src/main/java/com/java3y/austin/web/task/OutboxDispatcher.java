package com.java3y.austin.web.task;

/**
 * Outbox 事件分发器（P0-2c）
 * <p>
 * 屏蔽 MQ 差异：Kafka 走消息发送，EventBus 走进程内同步消费。
 *
 * @author sentinel
 */
public interface OutboxDispatcher {

    /**
     * 分发一条 outbox 事件，失败时抛异常由投递任务标记重试
     */
    void dispatch(String payload) throws Exception;
}