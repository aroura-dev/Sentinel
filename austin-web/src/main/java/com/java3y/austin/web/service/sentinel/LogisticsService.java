package com.java3y.austin.web.service.sentinel;

import com.alibaba.fastjson2.JSONObject;
import com.java3y.austin.logistics.dao.LogisticsDao;
import com.java3y.austin.logistics.enums.LogisticsNode;
import com.java3y.austin.logistics.enums.LogisticsNodeTransition;
import com.java3y.austin.logistics.model.LogisticsOrder;
import com.java3y.austin.logistics.model.LogisticsTrack;
import com.java3y.austin.logistics.spi.TrackGenerator;
import com.java3y.austin.web.service.AnomalyWorkflowService;
import com.java3y.austin.web.service.SentinelNotifyService;
import com.java3y.austin.web.service.sentinel.tms.WaybillService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * 物流领域服务：订单/轨迹/状态机推进的业务逻辑统一收敛于此
 * <p>
 * 从 LogisticsController 抽取。状态机推进时触发通知（正常节点）或异步异常工作流（异常节点），
 * 供手动推进接口与 {@code OrderAutoAdvanceTask} 共用，避免重复触发。
 *
 * @author sentinel
 */
@Service
public class LogisticsService {

    private static final Logger log = LoggerFactory.getLogger(LogisticsService.class);

    private final LogisticsDao logisticsDao;
    private final SentinelNotifyService notifyService;
    private final AnomalyWorkflowService anomalyWorkflowService;
    private final TrackGenerator trackGenerator;
    private final WaybillService waybillService;

    public LogisticsService(LogisticsDao logisticsDao,
                            SentinelNotifyService notifyService,
                            AnomalyWorkflowService anomalyWorkflowService,
                            TrackGenerator trackGenerator,
                            WaybillService waybillService) {
        this.logisticsDao = logisticsDao;
        this.notifyService = notifyService;
        this.anomalyWorkflowService = anomalyWorkflowService;
        this.trackGenerator = trackGenerator;
        this.waybillService = waybillService;
    }

    /**
     * Mock 创建物流订单（落库 DB 为唯一真相源）
     */
    public LogisticsOrder createOrder(String buyerId, String language, String country) {
        String orderNo = "OMT" + System.currentTimeMillis();
        LogisticsOrder order = LogisticsOrder.builder()
                .orderNo(orderNo)
                .buyerId(buyerId)
                .buyerLanguage(language)
                .destinationCountry(country)
                .currentNode(LogisticsNode.CREATED.getCodeEn())
                .build();
        logisticsDao.saveOrder(order);
        return order;
    }

    public Map<String, Object> getOrder(String orderNo) {
        return logisticsDao.findOrderByNo(orderNo);
    }

    /**
     * 状态机推进：合法转移 → 轨迹落库 → 节点更新 → 通知/异常工作流
     *
     * @return 下一条轨迹；订单不存在或已终态返回 null
     */
    public LogisticsTrack advance(String orderNo) {
        Map<String, Object> row = logisticsDao.findOrderByNo(orderNo);
        if (row == null) {
            return null;
        }
        LogisticsOrder order = toOrder(row);
        LogisticsTrack nextTrack = trackGenerator.nextTrack(order);
        if (nextTrack == null) {
            return null;
        }
        order.setCurrentNode(nextTrack.getNode());
        logisticsDao.saveTrack(nextTrack);
        logisticsDao.updateOrderCurrentNode(orderNo, nextTrack.getNode());

        // 妥投时回写运单
        if (nextTrack.getNode() != null && nextTrack.getNode().equals(LogisticsNode.DELIVERED.getCodeEn())) {
            waybillService.onDelivered(orderNo);
        }

        LogisticsNode nextNode = LogisticsNode.getByCodeEn(nextTrack.getNode());
        if (nextNode != null && !nextNode.isAnomaly()) {
            try {
                notifyService.send(orderNo, nextTrack.getNode(), "buyer", "push");
            } catch (Exception e) {
                log.warn("[Logistics] 状态推进通知触发失败 orderNo={}", orderNo, e);
            }
        }
        // 异常节点：异步触发异常工作流（诊断/文案/工单责任链）
        if (nextNode != null && nextNode.isAnomaly()) {
            final String node = nextTrack.getNode();
            final String lang = order.getBuyerLanguage() == null ? "zh" : order.getBuyerLanguage();
            CompletableFuture.runAsync(() -> anomalyWorkflowService.handleAnomaly(orderNo, node, lang));
        }
        return nextTrack;
    }

    public List<Map<String, Object>> tracks(String orderNo) {
        return logisticsDao.listTracks(orderNo);
    }

    public Map<String, Object> listOrders(String orderNo, String status, int page, int perPage) {
        return logisticsDao.listOrders(orderNo, status, page, perPage);
    }

    /**
     * 状态机可视化：所有节点 + 可转移的下一节点
     */
    public List<JSONObject> listNodes() {
        return Arrays.stream(LogisticsNode.values())
                .map(node -> {
                    JSONObject obj = new JSONObject();
                    obj.put("code", node.getCode());
                    obj.put("codeEn", node.getCodeEn());
                    obj.put("description", node.getDescription());
                    obj.put("isTerminal", node.isTerminal());
                    obj.put("isAnomaly", node.isAnomaly());
                    obj.put("nextNodes", LogisticsNodeTransition.nextNodes(node).stream()
                            .map(LogisticsNode::getCodeEn)
                            .collect(Collectors.toList()));
                    return obj;
                })
                .collect(Collectors.toList());
    }

    private LogisticsOrder toOrder(Map<String, Object> row) {
        return LogisticsOrder.builder()
                .orderNo(String.valueOf(row.get("order_no")))
                .buyerId(row.get("buyer_id") == null ? null : String.valueOf(row.get("buyer_id")))
                .buyerLanguage(row.get("buyer_language") == null ? "zh" : String.valueOf(row.get("buyer_language")))
                .destinationCountry(row.get("destination_country") == null ? null : String.valueOf(row.get("destination_country")))
                .currentNode(row.get("current_node") == null ? LogisticsNode.CREATED.getCodeEn() : String.valueOf(row.get("current_node")))
                .build();
    }
}
