package com.java3y.austin.web.service;

import com.java3y.austin.agent.agent.ContentGenAgent;
import com.java3y.austin.agent.service.AgentCallLogService;
import com.java3y.austin.common.enums.RespStatusEnum;
import com.java3y.austin.logistics.context.SentinelContext;
import com.java3y.austin.logistics.dao.LogisticsDao;
import com.java3y.austin.logistics.dao.NotificationDao;
import com.java3y.austin.service.api.domain.MessageParam;
import com.java3y.austin.service.api.domain.SendRequest;
import com.java3y.austin.service.api.domain.SendResponse;
import com.java3y.austin.service.api.enums.BusinessCode;
import com.java3y.austin.service.api.service.SendService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Map;

/**
 * Sentinel 通知服务
 * <p>
 * 统一封装「Agent 生成文案 + 通知落库 + 自动分发」逻辑，供手动发送接口
 * （/api/notification/send）、物流状态机推进触发与自动推进任务共同复用，
 * 打通「状态机 → 通知 → 发送链路」的业务闭环。
 * <p>
 * 链路 ID（trace_id）统一采用 SentinelContext 编码（orderNo|node|language|role），
 * 贯穿 Web 层与 Handler 层，保证 agent_call_log 与 notification_record 可关联追溯。
 *
 * @author sentinel
 */
@Service
public class SentinelNotifyService {

    private static final Logger log = LoggerFactory.getLogger(SentinelNotifyService.class);

    @Autowired
    private ContentGenAgent contentGenAgent;
    @Autowired
    private AgentCallLogService agentCallLogService;
    @Autowired
    private NotificationDao notificationDao;
    @Autowired
    private LogisticsDao logisticsDao;
    @Autowired
    private SendService sendService;

    /**
     * 通知自动分发开关
     */
    @Value("${sentinel.dispatch.enabled:true}")
    private boolean dispatchEnabled;
    /**
     * 分发使用的 austin 消息模板 id（需与 buyerId 的 idType 匹配，模板 3 为 uid 类型）
     */
    @Value("${sentinel.dispatch.template-id:0}")
    private long dispatchTemplateId;

    /**
     * 发送通知：24h 去重 → 查买家信息 → Agent 生成通知文案 → 落库 → 自动分发送链路
     *
     * @param orderNo 订单号
     * @param node    物流节点（LogisticsNode.codeEn）
     * @param role    通知角色（buyer/merchant）
     * @param channel 通知渠道（push/sms/email）
     * @return 插入的通知记录，重复通知或查询失败返回 null
     */
    public Map<String, Object> send(String orderNo, String node, String role, String channel) {
        return send(orderNo, node, role, channel, false, null);
    }

    /**
     * 发送通知：force=true 时跳过 24h 去重（用于失败通知补发）
     */
    public Map<String, Object> send(String orderNo, String node, String role, String channel, boolean force) {
        return send(orderNo, node, role, channel, force, null);
    }

    /**
     * 发送通知并沿用上层编排 traceId，保证 Agent、工单、通知可关联回放。
     */
    public Map<String, Object> send(String orderNo, String node, String role, String channel,
                                    boolean force, String traceId) {
        // 同一订单 + 节点 + 角色 24h 内去重，避免打扰用户（补发强制跳过）
        if (!force && notificationDao.existsRecent(orderNo, node, role, 24)) {
            log.info("[Notify] 24h 内已通知，跳过 orderNo={} node={} role={}", orderNo, node, role);
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
            if (order.get("buyer_id") != null) {
                buyerId = String.valueOf(order.get("buyer_id"));
            }
            productInfo = buildProductInfo(order);
        }

        SentinelContext ctx = SentinelContext.of(orderNo, node, language, role);
        String recordTraceId = traceId == null || traceId.trim().isEmpty() ? ctx.toBizId() : traceId;
        String content = contentGenAgent.generate(node, language, productInfo, orderNo, recordTraceId);
        notificationDao.insert(orderNo, node, role, channel, content, language, "PENDING", recordTraceId);

        Map<String, Object> latest = notificationDao.queryPage(orderNo, channel, null, 1, 1);
        Object rows = latest.get("rows");
        Map<String, Object> row = null;
        if (rows instanceof java.util.List && !((java.util.List<?>) rows).isEmpty()) {
            row = (Map<String, Object>) ((java.util.List<?>) rows).get(0);
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

    /**
     * 通知自动分发：把 PENDING 通知送入 austin 发送责任链
     * （SendService → API 责任链 → MQ → handler 责任链 → 渠道 Handler）。
     * <p>
     * bizId 编码为 SentinelContext（orderNo|node|language|role），使 Handler 层 Agent
     * 能还原真实上下文；发送为异步执行（eventBus 线程池），本方法仅回写提交状态。
     */
    private void dispatch(Map<String, Object> row, String buyerId, String orderNo, SentinelContext ctx) {
        if (!dispatchEnabled || dispatchTemplateId <= 0 || row == null) {
            return;
        }
        Long id = row.get("id") == null ? null : ((Number) row.get("id")).longValue();
        if (id == null || buyerId == null || buyerId.isEmpty() || "null".equals(buyerId)) {
            return;
        }
        try {
            MessageParam messageParam = MessageParam.builder()
                    .bizId(ctx.toBizId())
                    .receiver(buyerId)
                    .variables(Collections.singletonMap("orderNo", orderNo))
                    .build();
            SendRequest sendRequest = SendRequest.builder()
                    .code(BusinessCode.COMMON_SEND.getCode())
                    .messageTemplateId(dispatchTemplateId)
                    .messageParam(messageParam)
                    .build();
            SendResponse response = sendService.send(sendRequest);
            boolean accepted = response != null && RespStatusEnum.SUCCESS.getCode().equals(response.getCode());
            notificationDao.updateStatus(id, accepted ? "SENT" : "FAILED");
            log.info("[NotifyDispatch] 通知已分发送链路 id={} orderNo={} submitStatus={}",
                    id, orderNo, accepted ? "SENT" : "FAILED");
        } catch (Exception e) {
            log.error("[NotifyDispatch] 通知分发失败 id={} orderNo={}", id, orderNo, e);
            try {
                notificationDao.updateStatus(id, "FAILED");
            } catch (Exception ignored) {
            }
        }
    }
}
