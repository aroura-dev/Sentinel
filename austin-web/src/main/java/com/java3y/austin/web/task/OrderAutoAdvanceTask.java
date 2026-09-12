package com.java3y.austin.web.task;

import com.java3y.austin.logistics.dao.LogisticsDao;
import com.java3y.austin.logistics.enums.LogisticsNode;
import com.java3y.austin.logistics.spi.TrackGenerator;
import com.java3y.austin.web.service.sentinel.LogisticsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 物流订单自动推进任务（自动化闭环核心驱动）
 * <p>
 * 周期扫描所有非终态订单，按状态机合法转移自动推进一步。推进逻辑（轨迹落库/通知/异常工作流）
 * 统一委托 {@link LogisticsService#advance(String)}，与手动推进接口共用，避免重复触发。
 * <p>
 * 轨迹数据来自 Mock 数据源（模拟物流商推送）；接入真实物流商后替换为轨迹回调消费即可，下游链路不变。
 *
 * @author sentinel
 */
@Component
public class OrderAutoAdvanceTask {

    private static final Logger log = LoggerFactory.getLogger(OrderAutoAdvanceTask.class);

    /**
     * 非终态节点集合（终态 DELIVERED/LOST/RETURNED 不再推进）
     */
    private static final List<String> ACTIVE_NODES = Arrays.stream(LogisticsNode.values())
            .filter(node -> !node.isTerminal() && !node.isAnomaly())
            .map(LogisticsNode::getCodeEn)
            .collect(Collectors.toList());

    @Autowired
    private LogisticsDao logisticsDao;
    @Autowired
    private LogisticsService logisticsService;
    @Autowired
    private TrackGenerator trackGenerator;

    @Value("${sentinel.auto-advance.enabled:true}")
    private boolean enabled;

    /**
     * 是否尊重节点驻留时长（默认 false：演示保持快节奏推进；
     * 开启后按当前节点最小驻留时长跳过仍在窗口内的订单，更贴近真实物流时效）
     */
    @Value("${sentinel.auto-advance.respect-dwell:false}")
    private boolean respectDwell;

    @Scheduled(fixedDelayString = "${sentinel.auto-advance.interval-ms:30000}", initialDelay = 20_000)
    public void autoAdvance() {
        if (!enabled) {
            return;
        }
        try {
            List<Map<String, Object>> orders = logisticsDao.listOrdersByNodes(ACTIVE_NODES);
            if (orders == null || orders.isEmpty()) {
                return;
            }
            for (Map<String, Object> row : orders) {
                if (respectDwell && withinDwell(row)) {
                    continue;
                }
                String orderNo = String.valueOf(row.get("order_no"));
                try {
                    logisticsService.advance(orderNo);
                    log.info("[OrderAutoAdvance] 订单自动推进完成 orderNo={}", orderNo);
                } catch (Exception e) {
                    log.error("[OrderAutoAdvance] 单笔推进失败 orderNo={}", orderNo, e);
                }
            }
        } catch (Exception e) {
            log.error("[OrderAutoAdvance] 自动推进执行失败", e);
        }
    }

    /**
     * 判断订单是否仍处于当前节点的最小驻留窗口内（true=跳过推进）
     */
    private boolean withinDwell(Map<String, Object> row) {
        String node = row.get("current_node") == null ? LogisticsNode.CREATED.getCodeEn() : String.valueOf(row.get("current_node"));
        LogisticsNode current = LogisticsNode.getByCodeEn(node);
        if (current == null) {
            return false;
        }
        long dwell = trackGenerator.dwellMillis(current);
        if (dwell <= 0) {
            return false;
        }
        Object updated = row.get("updated_at");
        if (updated == null) {
            return false;
        }
        long updatedMs;
        if (updated instanceof java.util.Date) {
            updatedMs = ((java.util.Date) updated).getTime();
        } else if (updated instanceof Number) {
            updatedMs = ((Number) updated).longValue();
        } else {
            return false;
        }
        return (System.currentTimeMillis() - updatedMs) < dwell;
    }
}
