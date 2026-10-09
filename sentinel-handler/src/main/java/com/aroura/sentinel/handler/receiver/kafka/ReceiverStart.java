package com.aroura.sentinel.handler.receiver.kafka;

import cn.hutool.core.text.StrPool;
import com.aroura.sentinel.handler.utils.GroupIdMappingUtils;
import com.aroura.sentinel.support.constans.MessageQueuePipeline;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.header.Header;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.event.EventListener;
import org.springframework.kafka.annotation.KafkaListenerAnnotationBeanPostProcessor;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.stereotype.Service;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

/**
 * 启动消费者
 *
 * @author Sentinel
 * @date 2021/12/4
 */
@Service
@ConditionalOnProperty(name = "sentinel.mq.pipeline", havingValue = MessageQueuePipeline.KAFKA)
@Slf4j
public class ReceiverStart {

    /**
     * receiver的消费方法常量
     */
    private static final String RECEIVER_METHOD_NAME = "Receiver.consumer";
    /**
     * 获取得到所有的groupId
     */
    private static final List<String> GROUP_IDS = GroupIdMappingUtils.getAllGroupIds();
    /**
     * 下标(用于迭代groupIds位置)
     */
    private static Integer index = 0;
    @Autowired
    private ApplicationContext context;
    @Autowired
    private ConsumerFactory consumerFactory;

    /**
     * 给每个Receiver对象的consumer方法 @KafkaListener赋值相应的groupId
     */
    @Bean
    public static KafkaListenerAnnotationBeanPostProcessor.AnnotationEnhancer groupIdEnhancer() {
        return (attrs, element) -> {
            if (element instanceof Method) {
                String name = ((Method) element).getDeclaringClass().getSimpleName() + StrPool.DOT + ((Method) element).getName();
                if (RECEIVER_METHOD_NAME.equals(name)) {
                    attrs.put("groupId", GROUP_IDS.get(index++));
                }
            }
            return attrs;
        };
    }

    /**
     * 为每个渠道不同的消息类型 创建一个Receiver对象
     * <p>（sentinel-ms 改造）由 @PostConstruct 改为 ApplicationReadyEvent：
     * 在上下文就绪后再拉取原型 Receiver 启动各组 Kafka 监听器，避免在
     * ReceiverStart 自身创建过程中 getBean(Receiver) 造成的循环依赖。
     */
    @EventListener(ApplicationReadyEvent.class)
    public void init(ApplicationReadyEvent event) {
        for (int i = 0; i < GROUP_IDS.size(); i++) {
            context.getBean(Receiver.class);
        }
    }

    /**
     * 针对tag消息过滤
     * producer 将tag写进header里
     *
     * @return true 消息将会被丢弃
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory filterContainerFactory(@Value("${sentinel.business.tagId.key}") String tagIdKey,
                                                                          @Value("${sentinel.business.tagId.value}") String tagIdValue) {
        ConcurrentKafkaListenerContainerFactory<String, String> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        // 被 tag 过滤掉的消息无需处理，由容器直接提交
        factory.setAckDiscarded(true);

        /**
         * 位移提交语义（此前用的是默认自动提交，存在丢消息的窗口）：
         * <p>
         * 监听器把任务丢进线程池后就返回了，而自动提交发生在监听器返回时 ——
         * 等于消息还没发出去就承认它处理完了。进程若在两者之间崩溃，消息永久丢失。
         * <p>
         * 改为 MANUAL_IMMEDIATE：由任务执行完毕的回调提交（见 ConsumeServiceImpl）。
         * 同时必须开 asyncAcks —— 提交发生在业务线程池里而非监听线程上，
         * 缺少它时跨线程 commitSync 是未定义行为；开启后容器按位移顺序安全提交。
         */
        ContainerProperties containerProperties = factory.getContainerProperties();
        containerProperties.setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);
        containerProperties.setAsyncAcks(true);

        factory.setRecordFilterStrategy(consumerRecord -> {
            if (Optional.ofNullable(consumerRecord.value()).isPresent()) {
                for (Header header : consumerRecord.headers()) {
                    if (header.key().equals(tagIdKey) &&
                            new String(header.value(), StandardCharsets.UTF_8).equals(tagIdValue)) {
                        return false;
                    }
                }
            }
            return true;
        });
        return factory;
    }
}
