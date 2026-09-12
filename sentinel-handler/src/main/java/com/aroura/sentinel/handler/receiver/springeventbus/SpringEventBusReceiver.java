package com.aroura.sentinel.handler.receiver.springeventbus;

import com.aroura.sentinel.common.domain.RecallTaskInfo;
import com.aroura.sentinel.common.domain.TaskInfo;
import com.aroura.sentinel.handler.receiver.service.ConsumeService;
import com.aroura.sentinel.support.constans.MessageQueuePipeline;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 描述：
 *
 * @author tony
 * @date 2023/2/6 11:18
 */
@Component
@ConditionalOnProperty(name = "sentinel.mq.pipeline", havingValue = MessageQueuePipeline.SPRING_EVENT_BUS)
public class SpringEventBusReceiver {

    @Autowired
    private ConsumeService consumeService;

    public void consume(List<TaskInfo> lists) {
        consumeService.consume2Send(lists);
    }

    public void recall(RecallTaskInfo recallTaskInfo) {
        consumeService.consume2recall(recallTaskInfo);
    }
}
