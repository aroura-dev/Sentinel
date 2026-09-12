package com.aroura.sentinel.handler.receiver.rocketmq;

import com.alibaba.fastjson2.JSON;
import com.aroura.sentinel.common.domain.RecallTaskInfo;
import com.aroura.sentinel.handler.receiver.MessageReceiver;
import com.aroura.sentinel.handler.receiver.service.ConsumeService;
import com.aroura.sentinel.support.constans.MessageQueuePipeline;
import org.apache.commons.lang3.StringUtils;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.annotation.SelectorType;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Description:
 *
 * @author elpsycongroo
 * create date: 2022/7/16
 */
@Component
@ConditionalOnProperty(name = "sentinel.mq.pipeline", havingValue = MessageQueuePipeline.ROCKET_MQ)
@RocketMQMessageListener(topic = "${sentinel.business.recall.topic.name}",
        consumerGroup = "${sentinel.rocketmq.recall.consumer.group}",
        selectorType = SelectorType.TAG,
        selectorExpression = "${sentinel.business.tagId.value}"
)
public class RocketMqRecallReceiver implements RocketMQListener<String>, MessageReceiver {

    @Autowired
    private ConsumeService consumeService;

    @Override
    public void onMessage(String message) {
        if (StringUtils.isBlank(message)) {
            return;
        }
        RecallTaskInfo recallTaskInfo = JSON.parseObject(message, RecallTaskInfo.class);
        consumeService.consume2recall(recallTaskInfo);
    }
}
