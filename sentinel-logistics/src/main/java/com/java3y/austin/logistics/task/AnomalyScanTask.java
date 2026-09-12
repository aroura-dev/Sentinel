package com.java3y.austin.logistics.task;

import com.java3y.austin.logistics.dao.LogisticsDao;
import com.java3y.austin.logistics.model.LogisticsTrack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 物流异常扫描任务（PRD 4.3 / 6.3）
 * <p>
 * 修复：原项目无异常检测触发源。本任务周期扫描到达分拨（IMPORT_CUSTOMS）滞留超过
 * 阈值（默认 48 小时，可配置）的订单，自动推进到中转延误（CUSTOMS_DELAY）并生成轨迹。
 *
 * @author sentinel
 */
@Component
public class AnomalyScanTask {

    private static final Logger log = LoggerFactory.getLogger(AnomalyScanTask.class);

    /** 到达分拨滞留阈值（小时），可通过 sentinel.anomaly.import-customs-stuck-hours 配置 */
    @Value("${sentinel.anomaly.import-customs-stuck-hours:48}")
    private int customsStuckHours;

    @Autowired
    private LogisticsDao logisticsDao;

    @Scheduled(fixedDelay = 60_000, initialDelay = 30_000)
    public void scanCustomsDelay() {
        try {
            List<Map<String, Object>> stuck = logisticsDao.listStuckOrders("IMPORT_CUSTOMS", customsStuckHours);
            if (stuck == null || stuck.isEmpty()) {
                return;
            }
            for (Map<String, Object> order : stuck) {
                String orderNo = String.valueOf(order.get("order_no"));
                logisticsDao.updateOrderCurrentNode(orderNo, "CUSTOMS_DELAY");
                logisticsDao.saveTrack(LogisticsTrack.builder()
                        .orderNo(orderNo)
                        .node("CUSTOMS_DELAY")
                        .rawStatus("CUS-AUTO")
                        .rawDesc("到达分拨超过" + customsStuckHours + "小时，自动标记为中转延误")
                        .location("目的地分拨中心")
                        .trackTime(System.currentTimeMillis())
                        .build());
                log.info("[AnomalyScanTask] 订单 {} 到达分拨超时，自动标记 CUSTOMS_DELAY", orderNo);
            }
        } catch (Exception e) {
            log.error("[AnomalyScanTask] 异常扫描失败", e);
        }
    }
}