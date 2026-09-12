package com.aroura.sentinel.handler.action;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.aroura.sentinel.agent.agent.AnomalyDiagnoseAgent;
import com.aroura.sentinel.agent.service.AgentCallLogService;
import com.aroura.sentinel.common.domain.TaskInfo;
import com.aroura.sentinel.common.pipeline.BusinessProcess;
import com.aroura.sentinel.common.pipeline.ProcessContext;
import com.aroura.sentinel.logistics.context.SentinelContext;
import com.aroura.sentinel.logistics.dao.LogisticsDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Agent 调用 Action：异常诊断
 * <p>
 * 嵌入异常处理责任链，在 DiscardAction 之后执行。
 * 从 bizId 还原真实订单上下文，映射异常类型与知识库状态码，调用诊断 Agent（含知识库 RAG）。
 *
 * @author sentinel
 */
@Service
public class AgentDiagnoseAction implements BusinessProcess<TaskInfo> {

    private static final Logger log = LoggerFactory.getLogger(AgentDiagnoseAction.class);

    @Autowired
    private AnomalyDiagnoseAgent anomalyDiagnoseAgent;
    @Autowired
    private AgentCallLogService agentCallLogService;
    @Autowired
    private LogisticsDao logisticsDao;

    @Override
    public void process(ProcessContext<TaskInfo> context) {
        TaskInfo taskInfo = context.getProcessModel();
        String traceId = taskInfo.getBizId() != null ? taskInfo.getBizId() : agentCallLogService.generateTraceId();

        SentinelContext ctx = SentinelContext.fromBizId(taskInfo.getBizId());
        try {
            String anomalyType = SentinelContext.anomalyType(ctx.getNode());
            String statusCode = SentinelContext.anomalyStatusCode(ctx.getNode());
            JSONObject result = anomalyDiagnoseAgent.diagnose(anomalyType, statusCode, orderInfo(ctx.getOrderNo()), traceId);
            log.info("[AgentDiagnoseAction] 异常诊断完成 orderNo={} node={} type={} statusCode={} reason={} priority={}",
                    ctx.getOrderNo(), ctx.getNode(), anomalyType, statusCode,
                    result.getString("reason"), result.getString("priority"));
        } catch (Exception e) {
            log.error("[AgentDiagnoseAction] Agent 调用异常 orderNo={}", ctx.getOrderNo(), e);
        }
    }

    private String orderInfo(String orderNo) {
        if (orderNo == null) {
            return "{}";
        }
        try {
            Map<String, Object> order = logisticsDao.findOrderByNo(orderNo);
            return order == null ? "{}" : JSON.toJSONString(order);
        } catch (Exception e) {
            return "{}";
        }
    }
}
