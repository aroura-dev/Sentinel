package com.java3y.austin.web.task;

import com.java3y.austin.logistics.dao.LogisticsDao;
import com.java3y.austin.logistics.dao.WorkorderDao;
import com.java3y.austin.web.service.AnomalyWorkflowService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * 异常订单自动触发任务
 * <p>
 * 修复：PRD 4.3 要求"定时任务扫描 → 检测到异常 → 触发异常处理责任链"。
 * 本任务周期扫描处于异常节点且尚未建工单的订单，自动送入 AnomalyWorkflowService。
 * （物流层 AnomalyScanTask 负责把清关滞留>48h 订单标记为 CUSTOMS_DELAY，本任务消费其结果）
 *
 * @author sentinel
 */
@Component
public class AnomalyPipelineTrigger {

    private static final Logger log = LoggerFactory.getLogger(AnomalyPipelineTrigger.class);

    private static final List<String> ANOMALY_NODES =
            Arrays.asList("CUSTOMS_DELAY", "DELIVERY_FAILED", "LOST", "RETURNED");

    @Autowired
    private LogisticsDao logisticsDao;
    @Autowired
    private WorkorderDao workorderDao;
    @Autowired
    private AnomalyWorkflowService anomalyWorkflowService;

    @Scheduled(fixedDelay = 60_000, initialDelay = 15_000)
    public void triggerAnomalyPipeline() {
        try {
            List<Map<String, Object>> orders = logisticsDao.listOrdersByNodes(ANOMALY_NODES);
            if (orders == null || orders.isEmpty()) {
                return;
            }
            for (Map<String, Object> order : orders) {
                String orderNo = String.valueOf(order.get("order_no"));
                String node = String.valueOf(order.get("current_node"));
                Object langObj = order.get("buyer_language");
                String lang = langObj == null ? "zh" : String.valueOf(langObj);
                if (!workorderDao.existsByOrderNo(orderNo)) {
                    log.info("[AnomalyPipelineTrigger] 发现异常订单 orderNo={} node={}，触发责任链", orderNo, node);
                    anomalyWorkflowService.handleAnomaly(orderNo, node, lang);
                }
            }
        } catch (Exception e) {
            log.error("[AnomalyPipelineTrigger] 异常扫描触发失败", e);
        }
    }
}