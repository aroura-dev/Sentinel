package com.aroura.sentinel.handler.receiver.rabbit;

import com.alibaba.fastjson2.JSON;
import com.aroura.sentinel.common.domain.RecallTaskInfo;
import com.aroura.sentinel.common.domain.TaskInfo;
import com.aroura.sentinel.handler.receiver.MessageReceiver;
import com.aroura.sentinel.handler.receiver.service.ConsumeService;
import com.aroura.sentinel.support.constans.MessageQueuePipeline;
import org.apache.commons.lang3.StringUtils;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.List;


/**
 * @author xzcawl
 * @date 23-04-21 10:53:32
 */
@Component
@ConditionalOnProperty(name = "sentinel.mq.pipeline", havingValue = MessageQueuePipeline.RABBIT_MQ)
public class RabbitMqReceiver implements MessageReceiver {

    @Autowired
    private ConsumeService consumeService;

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "${spring.rabbitmq.queues.send}", durable = "true"),
            exchange = @Exchange(value = "${sentinel.rabbitmq.exchange.name}", type = ExchangeTypes.TOPIC),
            key = "${sentinel.rabbitmq.routing.send}"
    ))
    public void send(Message message) {
        byte[] body = message.getBody();
        String messageContent = new String(body, StandardCharsets.UTF_8);
        if (StringUtils.isBlank(messageContent)) {
            return;
        }
        // 处理发送消息
        List<TaskInfo> taskInfoLists = JSON.parseArray(messageContent, TaskInfo.class);
        consumeService.consume2Send(taskInfoLists);
    }

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "${spring.rabbitmq.queues.recall}", durable = "true"),
            exchange = @Exchange(value = "${sentinel.rabbitmq.exchange.name}", type = ExchangeTypes.TOPIC),
            key = "${sentinel.rabbitmq.routing.recall}"
    ))
    public void recall(Message message) {
        byte[] body = message.getBody();
        String messageContent = new String(body, StandardCharsets.UTF_8);
        if (StringUtils.isBlank(messageContent)) {
            return;
        }
        // 处理撤回消息
        RecallTaskInfo recallTaskInfo = JSON.parseObject(messageContent, RecallTaskInfo.class);
        consumeService.consume2recall(recallTaskInfo);
    }

}
