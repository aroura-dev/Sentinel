package com.aroura.sentinel.web.service.sentinel.tms;

import com.aroura.sentinel.logistics.dao.LogisticsDao;
import com.aroura.sentinel.logistics.enums.LogisticsNode;
import com.aroura.sentinel.logistics.model.LogisticsTrack;
import com.aroura.sentinel.web.exception.CommonException;
import com.aroura.sentinel.web.service.AnomalyWorkflowService;
import com.aroura.sentinel.web.service.SentinelNotifyService;
import com.aroura.sentinel.web.service.sentinel.LogisticsService;
import com.aroura.sentinel.web.support.TenantScopeResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * TMS 人工操作服务：手动推进节点 / 模拟异常 / 编辑订单
 * <p>
 * 让运营可以像真实业务一样手动驱动物流流程，而不是只靠定时任务。
 *
 * @author sentinel
 */
@Service
public class TmsOperationService {

    private static final Logger log = LoggerFactory.getLogger(TmsOperationService.class);

    private final LogisticsDao logisticsDao;
    private final LogisticsService logisticsService;
    private final AnomalyWorkflowService anomalyWorkflowService;
    private final SentinelNotifyService notifyService;
    private final AuditLogService auditLogService;
    private final TenantScopeResolver tenantScope;

    public TmsOperationService(LogisticsDao logisticsDao, LogisticsService logisticsService,
                               AnomalyWorkflowService anomalyWorkflowService, SentinelNotifyService notifyService,
                               AuditLogService auditLogService, TenantScopeResolver tenantScope) {
        this.logisticsDao = logisticsDao;
        this.logisticsService = logisticsService;
        this.anomalyWorkflowService = anomalyWorkflowService;
        this.notifyService = notifyService;
        this.auditLogService = auditLogService;
        this.tenantScope = tenantScope;
    }

    /**
     * 手动推进一个物流节点（校验状态机合法转移）
     */
    public Map<String, Object> advance(String orderNo) {
        Map<String, Object> order = logisticsDao.findOrderByNo(orderNo);
        if (order != null) {
            Object reviewStatus = order.get("review_status");
            if (reviewStatus != null && !"APPROVED".equals(String.valueOf(reviewStatus))) {
                throw new CommonException("订单未审核，请先审核通过");
            }
        }
        LogisticsTrack next = logisticsService.advance(orderNo);
        if (next == null) {
            throw new CommonException("订单不存在或已到终态");
        }
        auditLogService.log("order", "ADVANCE", orderNo, "人工推进节点 → " + next.getNode());
        Map<String, Object> result = new HashMap<>(4);
        result.put("orderNo", orderNo);
        result.put("node", next.getNode());
        result.put("rawStatus", next.getRawStatus());
        result.put("description", LogisticsNode.getByCodeEn(next.getNode()) == null
                ? next.getNode() : LogisticsNode.getByCodeEn(next.getNode()).getDescription());
        result.put("track", next);
        result.put("order", logisticsDao.findOrderByNo(orderNo));
        return result;
    }

    /**
     * 手动模拟异常：把订单强制置入异常节点，写轨迹 + 触发异常责任链
     */
    public Map<String, Object> injectAnomaly(String orderNo, String type) {
        Map<String, Object> order = logisticsDao.findOrderByNo(orderNo);
        if (order == null) {
            throw new CommonException("订单不存在");
        }
        // 归属断言紧跟取回之后，避免「校验」与「操作」之间订单被换手
        tenantScope.assertAccessible(merchantIdOf(order), "订单");
        LogisticsNode node = anomalyNodeOf(type);
        if (node == null) {
            throw new CommonException("非法异常类型: " + type + "（可选 customs_delay/delivery_failed/lost/returned）");
        }
        String rawStatus = anomalyStatusCode(type);
        String lang = order.get("buyer_language") == null ? "zh" : String.valueOf(order.get("buyer_language"));

        LogisticsTrack track = LogisticsTrack.builder()
                .orderNo(orderNo)
                .node(node.getCodeEn())
                .rawStatus(rawStatus)
                .rawDesc("人工模拟异常: " + node.getDescription())
                .location("人工操作")
                .trackTime(System.currentTimeMillis())
                .build();
        logisticsDao.saveTrack(track);
        logisticsDao.updateOrderCurrentNode(orderNo, node.getCodeEn());
        logisticsDao.updateSlaStatus(orderNo, "BREACHED");
        auditLogService.log("order", "ANOMALY", orderNo, "模拟异常 → " + node.getCodeEn() + "（触发诊断与工单）");

        try {
            notifyService.send(orderNo, node.getCodeEn(), "buyer", "push");
        } catch (Exception e) {
            log.warn("[TmsOperation] 异常通知触发失败 orderNo={}", orderNo, e);
        }
        // 触发异常责任链（诊断/文案/工单），与自动检测同一条链路
        CompletableFuture.runAsync(() -> {
            try {
                anomalyWorkflowService.handleAnomaly(orderNo, node.getCodeEn(), lang);
            } catch (Exception e) {
                log.warn("[TmsOperation] 异常工作流失败 orderNo={}", orderNo, e);
            }
        });

        Map<String, Object> result = new HashMap<>(4);
        result.put("orderNo", orderNo);
        result.put("node", node.getCodeEn());
        result.put("rawStatus", rawStatus);
        result.put("description", node.getDescription());
        result.put("order", logisticsDao.findOrderByNo(orderNo));
        return result;
    }

    /**
     * 人工编辑订单（买家/地址/语言/业务备注）。
     * 已出库订单收货信息已锁定（商家/收件人/地址不可改），仅允许补充业务备注。
     * 未提供的字段保持原值，避免只改业务备注时误清空收货信息。
     */
    public Map<String, Object> updateOrder(String orderNo, Map<String, Object> body) {
        Map<String, Object> order = logisticsDao.findOrderByNo(orderNo);
        if (order == null) {
            throw new CommonException("订单不存在");
        }
        // 归属断言紧跟取回之后，避免「校验」与「操作」之间订单被换手
        tenantScope.assertAccessible(merchantIdOf(order), "订单");
        boolean outbound = order.get("waybill_no") != null;
        String phone = body.containsKey("buyerPhone") ? String.valueOf(body.get("buyerPhone")) : strOrNull(order.get("buyer_phone"));
        String address = body.containsKey("buyerAddress") ? String.valueOf(body.get("buyerAddress")) : strOrNull(order.get("buyer_address"));
        String city = body.containsKey("buyerCity") ? String.valueOf(body.get("buyerCity")) : strOrNull(order.get("buyer_city"));
        String postal = body.containsKey("buyerPostal") ? String.valueOf(body.get("buyerPostal")) : strOrNull(order.get("buyer_postal"));
        String language = body.containsKey("buyerLanguage") ? String.valueOf(body.get("buyerLanguage")) : strOrNull(order.get("buyer_language"));
        String notes = body.containsKey("businessNotes") ? String.valueOf(body.get("businessNotes")) : strOrNull(order.get("business_notes"));
        boolean recvChanged = !String.valueOf(phone).equals(strOrNull(order.get("buyer_phone")))
                || !String.valueOf(address).equals(strOrNull(order.get("buyer_address")))
                || !String.valueOf(city).equals(strOrNull(order.get("buyer_city")))
                || !String.valueOf(postal).equals(strOrNull(order.get("buyer_postal")));
        if (outbound && recvChanged) {
            throw new CommonException("订单已出库，收货信息已锁定；如需修改收件人/地址，请走「售后退回」流程");
        }
        logisticsDao.updateOrderInfo(orderNo, phone, address, city, postal, language, notes);
        return logisticsDao.findOrderByNo(orderNo);
    }

    private static String strOrNull(Object v) {
        return v == null ? null : String.valueOf(v);
    }

    /**
     * 取消订单：仅允许未出库（CREATED 且未生成运单）直接取消。
     * 已出库订单需走售后退回流程，不允许直接取消。
     */
    public Map<String, Object> cancelOrder(String orderNo) {
        Map<String, Object> order = logisticsDao.findOrderByNo(orderNo);
        if (order == null) {
            throw new CommonException("订单不存在");
        }
        // 归属断言紧跟取回之后，避免「校验」与「操作」之间订单被换手
        tenantScope.assertAccessible(merchantIdOf(order), "订单");
        if (order.get("waybill_no") != null || !"CREATED".equals(String.valueOf(order.get("current_node")))) {
            throw new CommonException("订单已出库，不能直接取消，请走「售后退回」流程");
        }
        logisticsDao.updateOrderCurrentNode(orderNo, "CANCELED");
        logisticsDao.saveTrack(LogisticsTrack.builder()
                .orderNo(orderNo)
                .node("CANCELED")
                .rawStatus("EXP-0000")
                .rawDesc("订单已取消（未出库直接取消）")
                .location("人工操作")
                .trackTime(System.currentTimeMillis())
                .build());
        auditLogService.log("order", "CANCEL", orderNo, "取消订单");
        try {
            notifyService.send(orderNo, "CANCELED", "buyer", "push");
        } catch (Exception e) {
            log.warn("[TmsOperation] 取消通知触发失败 orderNo={}", orderNo, e);
        }
        return logisticsDao.findOrderByNo(orderNo);
    }

    private static LogisticsNode anomalyNodeOf(String type) {
        if (type == null) {
            return null;
        }
        switch (type) {
            case "customs_delay":
                return LogisticsNode.CUSTOMS_DELAY;
            case "delivery_failed":
                return LogisticsNode.DELIVERY_FAILED;
            case "lost":
                return LogisticsNode.LOST;
            case "returned":
                return LogisticsNode.RETURNED;
            default:
                return null;
        }
    }

    private static String anomalyStatusCode(String type) {
        switch (type) {
            case "customs_delay":
                return "CUS-1102";
            case "delivery_failed":
                return "EXP-0071";
            case "lost":
                return "EXP-0051";
            case "returned":
                return "EXP-0062";
            default:
                return "EXP-9999";
        }
    }

    private static Long merchantIdOf(Map<String, Object> row) {
        if (row == null || row.get("merchant_id") == null) {
            return null;
        }
        return Long.valueOf(String.valueOf(row.get("merchant_id")));
    }
}
