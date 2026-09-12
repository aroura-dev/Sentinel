package com.aroura.logistics.service;

import java.util.List;
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

    public Map<String, Object> send(String orderNo, String node, String role, String channel) {
        // 同一订单+节点+角色 在去重窗内已通知则跳过
        if (notificationDao.existsRecent(orderNo, node, role, dedupHours)) {
            log.info("[Notify] {}-小时内已通知，跳过 orderNo={} node={} role={}", dedupHours, orderNo, node, role);
            return null;
        }

        String language = "zh";
        String buyerId = null;
        String productInfo = "商品信息";
        Map<String, Object> order = logisticsDao.findOrderByNo(orderNo);
        if (order != null) {
            if (order.get("buyer_language") != null) {
                language = String.valueOf(order.get("buyer_language"));
            }
            // 接收方：buyer_phone 为数字串(手机号)时优先；否则退回 buyer_id(账号/姓名标识)
            String bp = order.get("buyer_phone") == null ? "" : String.valueOf(order.get("buyer_phone")).trim();
            Object bid = order.get("buyer_id");
            if (bp.matches("\\d{6,15}")) {
                buyerId = bp;
            } else if (bid != null && !String.valueOf(bid).trim().isEmpty() && !"null".equals(String.valueOf(bid))) {
                buyerId = String.valueOf(bid);
            }
            productInfo = buildProductInfo(order);
        }

        SentinelContext ctx = SentinelContext.of(orderNo, node, language, role);
        String traceId = ctx.toBizId();

        String content = agentClient.generate(orderNo, node, language, productInfo, traceId);
        if (content == null || content.trim().isEmpty()) {
            content = "您的订单 " + orderNo + " 物流状态已更新为：" + node;
        }
        notificationDao.insert(orderNo, node, role, channel, content, language, "PENDING", traceId);

        Map<String, Object> page = notificationDao.queryPage(orderNo, channel, null, 1, 1);
        Map<String, Object> row = null;
        Object rows = page == null ? null : page.get("rows");
        if (rows instanceof List && !((List<?>) rows).isEmpty()) {
            row = (Map<String, Object>) ((List<?>) rows).get(0);
        }
        dispatch(row, buyerId, orderNo, ctx);
        return row;
    }

    private String buildProductInfo(Map<String, Object> order) {
        String dest = order.get("destination_country") == null ? "" : String.valueOf(order.get("destination_country"));
        String merchant = order.get("merchant_name") == null ? "" : String.valueOf(order.get("merchant_name"));
        if (dest.isEmpty() && merchant.isEmpty()) {
            return "商品信息";
        }
        return "目的地:" + dest + ",商家:" + merchant;
    }

    private void dispatch(Map<String, Object> row, String buyerId, String orderNo, SentinelContext ctx) {
        if (!dispatchEnabled || dispatchTemplateId <= 0 || row == null) {
            return;
        }
        Long id = row.get("id") == null ? null : ((Number) row.get("id")).longValue();
        if (id == null || buyerId == null || buyerId.isEmpty() || "null".equals(buyerId)) {
            return;
        }
        try {
            boolean accepted = msgClient.send(dispatchTemplateId, ctx.toBizId(), buyerId, orderNo);
            notificationDao.updateStatus(id, accepted ? "SENT" : "FAILED");
            log.info("[NotifyDispatch] id={} orderNo={} submitStatus={}", id, orderNo, accepted ? "SENT" : "FAILED");
        } catch (Exception e) {
            log.error("[NotifyDispatch] 失败 id={} orderNo={}", id, orderNo, e);
            try {
                notificationDao.updateStatus(id, "FAILED");
            } catch (Exception ignored) {
            }
        }
    }
}
