package com.aroura.sentinel.handler.receiver.service;


import com.aroura.sentinel.common.domain.RecallTaskInfo;
import com.aroura.sentinel.common.domain.TaskInfo;

import org.springframework.kafka.support.Acknowledgment;

import java.util.List;

/**
 * 消费消息服务
 *
 * @author Sentinel
 */
public interface ConsumeService {

    /**
     * 从MQ拉到消息进行消费，发送消息。
     * <p>
     * 任务被投进线程池后监听器立刻返回，所以**位移必须由本方法在处理真正完成后手动提交**：
     * 依赖监听器返回时的自动提交，等于在消息还没发出去之前就承认它已被处理。
     *
     * @param taskInfoLists 本次记录拆出的任务
     * @param ack           全部任务结束（无论成功失败）后调用，用于提交位移
     */
    void consume2Send(List<TaskInfo> taskInfoLists, Acknowledgment ack);

    /**
     * 兼容其余 MQ 实现（RocketMQ / RabbitMQ / Redis / EventBus / SpringEventBus）。
     * <p>
     * 手动提交是 Kafka 特有的需求 —— 它的监听器把任务丢进线程池后立刻返回，
     * 而位移却在返回时自动提交。其余 MQ 各有自己的确认机制，不需要这个参数，
     * 传 null 即表示「无需提交」。
     */
    default void consume2Send(List<TaskInfo> taskInfoLists) {
        consume2Send(taskInfoLists, null);
    }


    /**
     * 从MQ拉到消息进行消费，撤回消息
     * 如果有 recallMessageId ，则优先撤回 recallMessageId
     * 如果没有 recallMessageId ，则撤回整个模板的消息
     *
     * @param recallTaskInfo
     */
    void consume2recall(RecallTaskInfo recallTaskInfo);


}
