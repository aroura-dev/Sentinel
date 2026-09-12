package com.aroura.sentinel.handler.receiver.springeventbus;

import com.alibaba.fastjson2.JSON;
import com.aroura.sentinel.common.domain.RecallTaskInfo;
import com.aroura.sentinel.common.domain.TaskInfo;
import com.aroura.sentinel.handler.receiver.MessageReceiver;
import com.aroura.sentinel.support.constans.MessageQueuePipeline;
import com.aroura.sentinel.support.mq.springeventbus.SentinelSpringEventBusEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Service;

/**
 * 描述：
 *
 * @author tony
 * @date 2023/2/6 11:19
 */
@Service
@ConditionalOnProperty(name = "sentinel.mq.pipeline", havingValue = MessageQueuePipeline.SPRING_EVENT_BUS)
public class SpringEventBusReceiverListener implements ApplicationListener<SentinelSpringEventBusEvent>, MessageReceiver {

    @Autowired
    private SpringEventBusReceiver springEventBusReceiver;

    @Value("${sentinel.business.topic.name}")
    private String sendTopic;
    @Value("${sentinel.business.recall.topic.name}")
    private String recallTopic;

    @Override
    public void onApplicationEvent(SentinelSpringEventBusEvent event) {
        String topic = event.getSentinelSpringEventSource().getTopic();
        String jsonValue = event.getSentinelSpringEventSource().getJsonValue();
        if (topic.equals(sendTopic)) {
            springEventBusReceiver.consume(JSON.parseArray(jsonValue, TaskInfo.class));
        } else if (topic.equals(recallTopic)) {
            springEventBusReceiver.recall(JSON.parseObject(jsonValue, RecallTaskInfo.class));
        }
    }
}
