package com.java3y.austin.web.task;

import com.java3y.austin.logistics.dao.LogisticsDao;
import com.java3y.austin.logistics.dao.WorkorderDao;
import com.java3y.austin.logistics.dao.tms.WaybillDao;
import com.java3y.austin.logistics.enums.LogisticsNode;
import com.java3y.austin.web.service.sentinel.tms.SlaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * SLA 风险扫描：在途订单超承诺时效 → RISK/BREACHED；首次 BREACHED 自动建 sla_breach 工单
 * <p>
 * 与 {@link com.java3y.austin.logistics.task.AnomalyScanTask}（清关滞留→CUSTOMS_DELAY）互补。
 *
 * @author sentinel
 */
@Component
public class SlaRiskScanTask {

    private static final Logger log = LoggerFactory.getLogger(SlaRiskScanTask.class);

    private final WaybillDao waybillDao;
    private final LogisticsDao logisticsDao;
    private final WorkorderDao workorderDao;
    private final SlaService slaService;

    public SlaRiskScanTask(WaybillDao waybillDao, LogisticsDao logisticsDao, WorkorderDao workorderDao, SlaService slaService) {
        this.waybillDao = waybillDao;
        this.logisticsDao = logisticsDao;
        this.workorderDao = workorderDao;
        this.slaService = slaService;
    }

    @Scheduled(fixedDelay = 60_000, initialDelay = 30_000)
    public void scan() {
        List<Map<String, Object>> orders = waybillDao.listActiveOrdersWithSla();
        for (Map<String, Object> row : orders) {
            try {
                String orderNo = String.valueOf(row.get("order_no"));
                String node = row.get("current_node") == null ? "" : String.valueOf(row.get("current_node"));
                int transitMax = row.get("transit_days_max") == null ? 10 : Integer.parseInt(String.valueOf(row.get("transit_days_max")));
                Date outboundAt = (Date) row.get("outbound_at");
                long elapsedHours = outboundAt == null ? 0
                        : (System.currentTimeMillis() - outboundAt.getTime()) / 3_600_000L;

                String status;
                LogisticsNode nodeEnum = LogisticsNode.getByCodeEn(node);
                if (nodeEnum != null && nodeEnum.isAnomaly()) {
                    status = "BREACHED";
                } else {
                    status = slaService.evaluate(transitMax, elapsedHours);
                }

                logisticsDao.updateSlaStatus(orderNo, status);
                if ("BREACHED".equals(status) && !workorderDao.existsByOrderNo(orderNo)) {
                    workorderDao.insert(orderNo, "sla_breach", "P1",
                            "SLA 违约：订单超承诺时效（渠道承诺 " + transitMax + " 天，已滞留 " + elapsedHours / 24 + " 天）",
                            null, null, "OPEN");
                    log.info("[SlaRiskScan] 自动创建 SLA 违约工单 orderNo={}", orderNo);
                }
            } catch (Exception e) {
                log.warn("[SlaRiskScan] 处理异常 orderNo={}", row.get("order_no"), e);
            }
        }
    }
}
