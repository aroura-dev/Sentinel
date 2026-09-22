package com.aroura.logistics.task;

import java.util.List;
import java.util.Map;

import com.aroura.logistics.service.LogisticsNotifyService;
import com.aroura.sentinel.logistics.dao.NotificationDao;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 通知投递补偿。
 * <p>
 * 补两件事，它们是同一个问题的两面 —— 通知发出去了没人确认：
 * <ol>
 *   <li><b>回收卡住的 PENDING</b>：进程在「落库」与「投递」之间崩溃会留下永久 PENDING。
 *       超过阈值仍未完成投递的，转 FAILED 交给下面的重投路径。</li>
 *   <li><b>重投 FAILED</b>：msg-service 不可达或拒绝受理时记录会置 FAILED。
 *       按 {@code next_retry_at} 的指数退避重投，超过 {@code notify.retry.max-attempts} 后停在 FAILED，
 *       由人介入 —— 无上限重投会把一个坏消息变成持续的打点噪音。</li>
 * </ol>
 * <p>
 * 在这之前两者都无人处理：投递失败的通知永久丢失，卡住的 PENDING 也永远不回收。
 *
 * <h3>已知限制</h3>
 * 未做分布式互斥，按单实例部署设计。多实例时同一个 FAILED 记录会被多个进程同时捞起、
 * 造成重复投递 —— 与 msg-service 消费端去重可以部分兜底，但正确做法是给领取动作加
 * 原子认领（{@code UPDATE ... WHERE status='FAILED'}）或引入分布式锁。属于多实例改造的一部分。
 *
 * @author sentinel-ms
 */
@Component
public class NotificationRetryTask {

    private static final Logger log = LoggerFactory.getLogger(NotificationRetryTask.class);

    @Autowired
    private NotificationDao notificationDao;
    @Autowired
    private LogisticsNotifyService notifyService;

    @Value("${notify.retry.enabled:true}")
    private boolean enabled;
    /** PENDING 超过该分钟数仍未投递完成，视为「卡住」并转 FAILED。 */
    @Value("${notify.retry.stale-pending-minutes:10}")
    private int stalePendingMinutes;
    /** 单轮最多回收多少条卡住的 PENDING / 重投多少条 FAILED，避免一轮里把下游打满。 */
    @Value("${notify.retry.batch-size:50}")
    private int batchSize;
    /**
     * 与 LogisticsNotifyService 共用的重试上限。这里也必须知道它：
     * 超过上限的记录 next_retry_at 为空、不会被 findRetryable 捞起，
     * 但显式传上限能让 SQL 意图自明，也避免上限调小时旧值被漏捞。
     */
    @Value("${notify.retry.max-attempts:5}")
    private int maxAttempts;

    @Scheduled(fixedDelayString = "${notify.retry.scan-interval-ms:60000}", initialDelayString = "${notify.retry.initial-delay-ms:30000}")
    public void compensate() {
        if (!enabled) {
            return;
        }
        try {
            reapStalePending();
            retryFailed();
        } catch (Exception e) {
            // 定时任务里抛异常会让后续调度静默停止，这里兜住并记录
            log.error("[NotifyRetry] 补偿轮次异常", e);
        }
    }

    private void reapStalePending() {
        int reaped = notificationDao.reapStalePending(stalePendingMinutes, batchSize);
        if (reaped > 0) {
            log.warn("[NotifyRetry] 回收 {} 条卡住的 PENDING（超过 {} 分钟未完成投递），转入重投",
                    reaped, stalePendingMinutes);
        }
    }

    private void retryFailed() {
        List<Map<String, Object>> rows = notificationDao.findRetryable(maxAttempts, batchSize);
        if (rows.isEmpty()) {
            return;
        }
        log.info("[NotifyRetry] 本轮重投 {} 条失败通知", rows.size());
        for (Map<String, Object> row : rows) {
            try {
                notifyService.retryOne(row);
            } catch (Exception e) {
                // 单条重投失败不能影响同批其他记录
                log.error("[NotifyRetry] 重投异常 id={}", row.get("id"), e);
            }
        }
    }
}
