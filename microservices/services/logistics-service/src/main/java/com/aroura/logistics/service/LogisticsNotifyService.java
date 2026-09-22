package com.aroura.logistics.service;

import java.sql.Timestamp;
import java.util.Map;

import com.aroura.sentinel.logistics.context.SentinelContext;
import com.aroura.sentinel.logistics.dao.LogisticsDao;
import com.aroura.sentinel.logistics.dao.NotificationDao;
import com.aroura.logistics.client.AgentClient;
import com.aroura.logistics.client.MsgClient;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * 通知闭环编排（logistics-service 侧）。
 * <p>
 * 由单体 SentinelNotifyService 拆分而来：
 * - 「Agent 生成文案」内联调用 → AgentClient(REST agent-service)；
 * - 「sendService.send 分发」内联调用 → MsgClient(REST msg-service)。
 * 本进程只负责：去重 → 取订单买家上下文 → 生成文案 → 落 notification_record(PENDING)
 * → 投递 msg → 回写状态。traceId 沿用 SentinelContext(orderNo|node|lang|role)，
 * 使 agent_call_log / sms_record / notification_record 可关联。
 * <p>
 * 状态语义（补偿任务依赖它区分「该重投」与「本就不该发」）：
 * SENT     投递已被 msg-service 受理
 * PENDING  刚落库、投递尚未完成；只应短暂存在
 * SKIPPED  没有可用接收方或投递未启用 —— 终态，不重投
 * FAILED   投递失败，由 NotificationRetryTask 按退避重投，超过上限后停在此状态
 *
 * @author sentinel-ms
 */
@Service
public class LogisticsNotifyService {

    private static final Logger log = LoggerFactory.getLogger(LogisticsNotifyService.class);

    @Autowired
    private NotificationDao notificationDao;
    @Autowired
    private LogisticsDao logisticsDao;
    @Autowired
    private AgentClient agentClient;
    @Autowired
    private MsgClient msgClient;

    @Value("${notify.dispatch.enabled:true}")
    private boolean dispatchEnabled;
    @Value("${notify.dispatch.template-id:3}")
    private long dispatchTemplateId;
    @Value("${notify.dedup-hours:24}")
    private int dedupHours;

    /** 投递失败后的最大重试次数（含首次失败），达到后不再重投。 */
    @Value("${notify.retry.max-attempts:5}")
    private int maxAttempts;
    /** 退避基数（秒），实际等待为 base * 2^(已重试次数)。 */
    @Value("${notify.retry.base-backoff-seconds:30}")
    private long baseBackoffSeconds;

    public Map<String, Object> send(String orderNo, String node, String role, String channel) {
        // 同一订单+节点+角色 在去重窗内已通知则跳过（FAILED/SKIPPED 不计入，见 NotificationDao#existsRecent）
        if (notificationDao.existsRecent(orderNo, node, role, dedupHours)) {
            log.info("[Notify] {}-小时内已通知，跳过 orderNo={} node={} role={}", dedupHours, orderNo, node, role);
            return null;
        }

        String language = "zh";
        String productInfo = "商品信息";
        Map<String, Object> order = logisticsDao.findOrderByNo(orderNo);
        if (order != null) {
            if (order.get("buyer_language") != null) {
                language = String.valueOf(order.get("buyer_language"));
            }
            productInfo = buildProductInfo(order);
        }
        String buyerId = resolveRecipient(order);

        SentinelContext ctx = SentinelContext.of(orderNo, node, language, role);
        String traceId = ctx.toBizId();

        String content = agentClient.generate(orderNo, node, language, productInfo, traceId);
        if (content == null || content.trim().isEmpty()) {
            content = "您的订单 " + orderNo + " 物流状态已更新为：" + node;
        }

        // 直接取自增主键定位本条记录。原实现改为回查「同 order_no 的最新一行」，
        // 那是前导通配符全表扫，并发下还可能拿到别人的行、把状态改到错误对象上。
        long id = notificationDao.insert(orderNo, node, role, channel, content, language, "PENDING", traceId);
        dispatch(id, buyerId, orderNo, ctx);
        return id > 0 ? notificationDao.queryById(id) : null;
    }

    /**
     * 补偿入口：对一条已存在的通知记录重新投递。
     * <p>
     * 接收方按订单号重新推导 —— 首次失败可能是因为订单当时还没有可用手机号，
     * 之后补全了就该能发出去。
     */
    public void retryOne(Map<String, Object> row) {
        long id = ((Number) row.get("id")).longValue();
        String orderNo = String.valueOf(row.get("order_no"));
        String node = String.valueOf(row.get("node"));
        String role = String.valueOf(row.get("role"));
        String language = row.get("language") == null ? "zh" : String.valueOf(row.get("language"));
        SentinelContext ctx = SentinelContext.of(orderNo, node, language, role);
        dispatch(id, resolveRecipient(logisticsDao.findOrderByNo(orderNo)), orderNo, ctx);
    }

    /** 接收方：buyer_phone 为手机号时优先，否则退回 buyer_id；都没有则返回 null。 */
    private String resolveRecipient(Map<String, Object> order) {
        if (order == null) {
            return null;
        }
        String bp = order.get("buyer_phone") == null ? "" : String.valueOf(order.get("buyer_phone")).trim();
        if (bp.matches("\\d{6,15}")) {
            return bp;
        }
        Object bid = order.get("buyer_id");
        if (bid != null && !String.valueOf(bid).trim().isEmpty() && !"null".equals(String.valueOf(bid))) {
            return String.valueOf(bid);
        }
        return null;
    }

    private String buildProductInfo(Map<String, Object> order) {
        String dest = order.get("destination_country") == null ? "" : String.valueOf(order.get("destination_country"));
        String merchant = order.get("merchant_name") == null ? "" : String.valueOf(order.get("merchant_name"));
        if (dest.isEmpty() && merchant.isEmpty()) {
            return "商品信息";
        }
        return "目的地:" + dest + ",商家:" + merchant;
    }

    /**
     * 投递到 msg-service 并回写状态。
     * <p>
     * 「本就不该发」必须显式落成 SKIPPED，不能留在 PENDING ——
     * 否则补偿任务无法区分它和「落库后进程崩了」，会把无接收方的记录反复重投。
     */
    private void dispatch(long id, String buyerId, String orderNo, SentinelContext ctx) {
        if (id <= 0) {
            return;
        }
        if (!dispatchEnabled) {
            notificationDao.markSkipped(id, "notify.dispatch.enabled=false");
            return;
        }
        if (dispatchTemplateId <= 0) {
            notificationDao.markSkipped(id, "notify.dispatch.template-id 未配置");
            return;
        }
        if (buyerId == null || buyerId.isEmpty() || "null".equals(buyerId)) {
            notificationDao.markSkipped(id, "订单无可用通知接收方（buyer_phone 与 buyer_id 均为空）");
            return;
        }
        try {
            boolean accepted = msgClient.send(dispatchTemplateId, ctx.toBizId(), buyerId, orderNo);
            if (accepted) {
                notificationDao.markSent(id);
                log.info("[NotifyDispatch] id={} orderNo={} submitStatus=SENT", id, orderNo);
            } else {
                scheduleRetry(id, orderNo, "msg-service 拒绝受理（send 未返回成功）");
            }
        } catch (Exception e) {
            log.error("[NotifyDispatch] 失败 id={} orderNo={}", id, orderNo, e);
            scheduleRetry(id, orderNo, e.getMessage());
        }
    }

    /** 登记失败并按指数退避安排下一次重试；超过上限则停在 FAILED 等人工介入。 */
    private void scheduleRetry(long id, String orderNo, String error) {
        Map<String, Object> row = notificationDao.queryById(id);
        int attempts = 0;
        if (row != null && row.get("retry_count") != null) {
            attempts = ((Number) row.get("retry_count")).intValue();
        }
        if (attempts + 1 >= maxAttempts) {
            // nextRetryAt 传 null：补偿任务只捞 next_retry_at 非空的记录，这里即退出补偿循环
            notificationDao.markFailed(id, error + "（已达重试上限 " + maxAttempts + " 次，停止自动补偿）", null);
            log.warn("[NotifyDispatch] id={} orderNo={} 重试已达上限 {} 次，停止补偿", id, orderNo, maxAttempts);
            return;
        }
        // 退避：base * 2^attempts，上限 2^10 倍避免溢出
        long backoffSeconds = baseBackoffSeconds * (1L << Math.min(attempts, 10));
        Timestamp nextRetryAt = new Timestamp(System.currentTimeMillis() + backoffSeconds * 1000L);
        notificationDao.markFailed(id, error, nextRetryAt);
        log.warn("[NotifyDispatch] id={} orderNo={} 第 {} 次失败，{}s 后重试", id, orderNo, attempts + 1, backoffSeconds);
    }
}
