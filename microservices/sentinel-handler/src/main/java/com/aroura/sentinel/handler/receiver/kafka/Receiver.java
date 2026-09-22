package com.aroura.sentinel.handler.receiver.kafka;

import cn.hutool.core.collection.CollUtil;
import com.alibaba.fastjson2.JSON;
import com.aroura.sentinel.common.domain.RecallTaskInfo;
import com.aroura.sentinel.common.domain.TaskInfo;
import com.aroura.sentinel.handler.receiver.MessageReceiver;
import com.aroura.sentinel.handler.receiver.service.ConsumeService;
import com.aroura.sentinel.handler.utils.GroupIdMappingUtils;
import com.aroura.sentinel.support.constans.MessageQueuePipeline;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Scope;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * @author Sentinel
 * 消费MQ的消息
 */
@Slf4j
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@ConditionalOnProperty(name = "sentinel.mq.pipeline", havingValue = MessageQueuePipeline.KAFKA)
public class Receiver implements MessageReceiver {
    @Autowired
    private ConsumeService consumeService;

    /**
     * 发送消息
     *
     * @param consumerRecord
     * @param topicGroupId
     */
    @KafkaListener(topics = "#{'${sentinel.business.topic.name}'}", containerFactory = "filterContainerFactory")
    public void consumer(ConsumerRecord<?, String> consumerRecord,
                         @Header(KafkaHeaders.GROUP_ID) String topicGroupId,
                         Acknowledgment ack) {
        Optional<String> kafkaMessage = Optional.ofNullable(consumerRecord.value());
        if (!kafkaMessage.isPresent()) {
            // 空值没有处理可言，直接提交；不提交会让位移停在这条上
            ack.acknowledge();
            return;
        }
        List<TaskInfo> taskInfoLists;
        try {
            taskInfoLists = JSON.parseArray(kafkaMessage.get(), TaskInfo.class);
        } catch (Exception e) {
            // 反序列化不了的消息永远不可能处理成功：重试只会无限循环。
            // 直接提交并留日志 —— 真正的兜底是接到死信主题，这里先保证不阻塞分区。
            log.error("[Receiver] 消息反序列化失败，已跳过 topic={} offset={}",
                    consumerRecord.topic(), consumerRecord.offset(), e);
            ack.acknowledge();
            return;
        }
        String messageGroupId;
        try {
            messageGroupId = GroupIdMappingUtils.getGroupIdByTaskInfo(CollUtil.getFirst(taskInfoLists.iterator()));
        } catch (Exception e) {
            log.error("[Receiver] 解析消费者组失败，已跳过 topic={} offset={}",
                    consumerRecord.topic(), consumerRecord.offset(), e);
            ack.acknowledge();
            return;
        }
        /**
         * 每个消费者组 只消费 他们自身关心的消息
         */
        if (topicGroupId.equals(messageGroupId)) {
            try {
                // 位移由 consume2Send 在所有任务真正结束后提交，不是在这里
                consumeService.consume2Send(taskInfoLists, ack);
            } catch (Exception e) {
                // 提交失败（线程池已关闭等）必须兜底提交，否则位移永远不推进，
                // 整个分区会卡死在这条消息上。业务侧另有补偿任务覆盖丢失的通知。
                log.error("[Receiver] 任务提交失败，已跳过 topic={} offset={}",
                        consumerRecord.topic(), consumerRecord.offset(), e);
                ack.acknowledge();
            }
        } else {
            // 不属于本组的消息，跳过但要提交
            ack.acknowledge();
        }
    }

    /**
     * 撤回消息
     *
     * @param consumerRecord
     */
    /**
     * 撤回消息。
     * <p>
     * 必须收下 Acknowledgment：本容器的提交模式是 MANUAL_IMMEDIATE，
     * 不带这个参数的监听器永远不会提交位移，撤回主题会彻底停住。
     * 撤回是同步处理的，所以处理完直接提交即可。
     */
    @KafkaListener(topics = "#{'${sentinel.business.recall.topic.name}'}", groupId = "#{'${sentinel.business.recall.group.name}'}", containerFactory = "filterContainerFactory")
    public void recall(ConsumerRecord<?, String> consumerRecord, Acknowledgment ack) {
        try {
            Optional<String> kafkaMessage = Optional.ofNullable(consumerRecord.value());
            if (kafkaMessage.isPresent()) {
                RecallTaskInfo recallTaskInfo = JSON.parseObject(kafkaMessage.get(), RecallTaskInfo.class);
                consumeService.consume2recall(recallTaskInfo);
            }
        } catch (Exception e) {
            log.error("[Receiver] 撤回消息处理失败，已跳过 topic={} offset={}",
                    consumerRecord.topic(), consumerRecord.offset(), e);
        } finally {
            ack.acknowledge();
        }
    }
}
