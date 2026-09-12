package com.aroura.sentinel.support.mq.eventbus;

import com.alibaba.fastjson2.JSON;
import com.google.common.eventbus.EventBus;
import com.aroura.sentinel.common.domain.RecallTaskInfo;
import com.aroura.sentinel.common.domain.TaskInfo;
import com.aroura.sentinel.support.constans.MessageQueuePipeline;
import com.aroura.sentinel.support.mq.SendMqService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;


/**
 * @author Sentinel
 * EventBus 发送实现类
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "sentinel.mq.pipeline", havingValue = MessageQueuePipeline.EVENT_BUS)
public class EventBusSendMqServiceImpl implements SendMqService {
    private final EventBus eventBus = new EventBus();

    @Autowired
    private EventBusListener eventBusListener;
    @Value("${sentinel.business.topic.name}")
    private String sendTopic;
    @Value("${sentinel.business.recall.topic.name}")
    private String recallTopic;

    /**
     * 单机 队列默认不支持 tagId过滤（单机无必要）
     *
     * @param topic
     * @param jsonValue
     * @param tagId
     */
    @Override
    public void send(String topic, String jsonValue, String tagId) {
        eventBus.register(eventBusListener);
        if (topic.equals(sendTopic)) {
            eventBus.post(JSON.parseArray(jsonValue, TaskInfo.class));
        } else if (topic.equals(recallTopic)) {
            eventBus.post(JSON.parseObject(jsonValue, RecallTaskInfo.class));
        }
    }

    @Override
    public void send(String topic, String jsonValue) {
        send(topic, jsonValue, null);
    }
}
