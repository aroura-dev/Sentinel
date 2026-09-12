package com.aroura.sentinel.web.task;

import com.aroura.sentinel.logistics.dao.OutboxEventDao;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 发件箱投递任务（P0-2）
 * <p>
 * 扫描 NEW 事件发送到 MQ，成功置 SENT；失败指数退避重试，超过上限置 DEAD。
 *
 * @author sentinel
 */
@Slf4j
@Component
public class OutboxEventPublisher {

    private static final int MAX_RETRY = 8;

    private final OutboxEventDao outboxEventDao;
    private final OutboxDispatcher outboxDispatcher;


    @Value("${sentinel.outbox.batch-size:50}")
    private int batchSize;

    public OutboxEventPublisher(OutboxEventDao outboxEventDao, OutboxDispatcher outboxDispatcher) {
        this.outboxEventDao = outboxEventDao;
        this.outboxDispatcher = outboxDispatcher;
    }

    @Scheduled(fixedDelayString = "${sentinel.outbox.publish-interval-ms:5000}")
    public void publishPending() {
        List<Map<String, Object>> pending = outboxEventDao.fetchPending(batchSize);
        for (Map<String, Object> row : pending) {
            Long id = ((Number) row.get("id")).longValue();
            int retry = row.get("retry_count") == null ? 0 : ((Number) row.get("retry_count")).intValue();
            try {
                outboxDispatcher.dispatch(String.valueOf(row.get("payload")));
                outboxEventDao.markSent(id);
            } catch (Exception e) {
                boolean dead = isDead(retry + 1);
                outboxEventDao.markFailed(id, e.getMessage(), nextRetrySeconds(retry), dead);
                log.warn("[Outbox] 投递失败 id={} retry={} dead={} err={}", id, retry + 1, dead, e.getMessage());
            }
        }
    }

    /**
     * 指数退避：5s、10s、20s…上限 300s
     */
    static int nextRetrySeconds(int retry) {
        long v = 5L << Math.min(Math.max(retry, 0), 6);
        return (int) Math.min(300, v);
    }

    static boolean isDead(int retryCount) {
        return retryCount >= MAX_RETRY;
    }
}