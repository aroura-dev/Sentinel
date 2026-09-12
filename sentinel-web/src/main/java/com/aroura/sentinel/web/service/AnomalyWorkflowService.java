package com.aroura.sentinel.web.service;

import com.google.common.collect.Sets;
import com.aroura.sentinel.common.domain.TaskInfo;
import com.aroura.sentinel.common.dto.model.EmailContentModel;
import com.aroura.sentinel.common.pipeline.ProcessContext;
import com.aroura.sentinel.common.pipeline.ProcessController;
import com.aroura.sentinel.handler.config.TaskPipelineConfig;
import com.aroura.sentinel.logistics.context.SentinelContext;
import com.aroura.sentinel.logistics.dao.LogisticsDao;
import com.aroura.sentinel.logistics.dao.WorkorderDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * 物流异常工作流（PRD 4.3）
 * <p>
 * 修复：原异常扫描只改订单节点/轨迹，未触发通知/工单责任链，业务闭环断裂。
 * 本服务把异常订单送入 sentinel Handler 层 anomalyTemplate 责任链
 * （Discard → AgentDiagnose → AgentContent → AgentWorkorder → SendMessage），
 * 责任链内 Agent 调用全部落 agent_call_log，文案/工单结果由 Action 落库。
 * 幂等：已存在工单的订单跳过，避免重复触发。
 *
 * @author sentinel
 */
@Service
public class AnomalyWorkflowService {

    private static final Logger log = LoggerFactory.getLogger(AnomalyWorkflowService.class);

    @Autowired
    @Qualifier("handlerProcessController")
    private ProcessController handlerProcessController;

    @Autowired
    private WorkorderDao workorderDao;
    @Autowired
    private LogisticsDao logisticsDao;

    public void handleAnomaly(String orderNo, String node, String language) {
        if (workorderDao.existsByOrderNo(orderNo)) {
            log.info("[AnomalyWorkflow] 订单 {} 已存在工单，跳过重复触发", orderNo);
            return;
        }
        String buyerId = "buyer_" + orderNo;
        try {
            Map<String, Object> order = logisticsDao.findOrderByNo(orderNo);
            if (order != null && order.get("buyer_id") != null) {
                buyerId = String.valueOf(order.get("buyer_id"));
            }
        } catch (Exception ignored) {
        }
        String lang = (language == null || language.isEmpty()) ? "zh" : language;

        TaskInfo taskInfo = TaskInfo.builder()
                .bizId(SentinelContext.of(orderNo, node, lang, "buyer").toBizId())
                .messageId(orderNo)
                .messageTemplateId(0L)
                .receiver(Sets.newHashSet(buyerId))
                .idType(10)
                .sendChannel(40)
                .templateType(10)
                .msgType(10)
                .shieldType(10)
                .contentModel(EmailContentModel.builder()
                        .title("物流异常通知")
                        .content("您的包裹出现异常，请关注最新进展。")
                        .build())
                .sendAccount(1)
                .build();

        ProcessContext context = ProcessContext.builder()
                .code(TaskPipelineConfig.PIPELINE_ANOMALY_CODE)
                .processModel(taskInfo)
                .needBreak(false)
                .build();

        log.info("[AnomalyWorkflow] 触发异常责任链 orderNo={} node={} buyer={}", orderNo, node, buyerId);
        handlerProcessController.process(context);
    }
}