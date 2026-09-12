package com.java3y.austin.web.service.sentinel;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.java3y.austin.logistics.dao.ConsumedEventDao;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 工单领域事件处理器（P0-2b）
 * <p>
 * 负责解析 outbox 事件、消费幂等（consumed_event 唯一键）与业务联动入口。
 * 与 MQ 解耦：Kafka/EventBus 适配器只负责把消息交给本处理器。
 *
 * @author sentinel
 */
@Slf4j
@Component
public class WorkOrderEventHandler {

    /** 消费者标识：同一事件被不同消费者消费时互不影响 */
    public static final String CONSUMER = "sentinel-workorder-outbox";

    private final ConsumedEventDao consumedEventDao;

    public WorkOrderEventHandler(ConsumedEventDao consumedEventDao) {
        this.consumedEventDao = consumedEventDao;
    }

    /**
     * 处理一条事件消息
     *
     * @return true=本次完成处理；false=重复事件/非法消息（已忽略）
     */
    public boolean handle(String message) {
        JSONObject event;
        try {
            event = JSON.parseObject(message);
        } catch (Exception e) {
            log.warn("[Outbox] 事件解析失败，已忽略: {}", e.getMessage());
            return false;
        }
        if (event == null) {
            return false;
        }
        String eventId = event.getString("eventId");
        if (eventId == null || eventId.trim().isEmpty()) {
            log.warn("[Outbox] 事件缺少 eventId，已忽略");
            return false;
        }
        // 消费幂等：唯一键冲突即重复事件
        if (!consumedEventDao.tryConsume(eventId, CONSUMER)) {
            log.info("[Outbox] 重复事件已幂等忽略 eventId={}", eventId);
            return false;
        }
        log.info("[Outbox] 处理工单状态事件 eventId={} orderNo={} {} -> {}",
                eventId, event.getString("orderNo"), event.getString("from"), event.getString("to"));
        // P0-2b 业务联动入口：后续在此触发通知/工单状态联动
        return true;
    }
}