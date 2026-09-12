package com.java3y.austin.handler.action;

import com.alibaba.fastjson2.JSONObject;
import com.java3y.austin.agent.agent.WorkorderAgent;
import com.java3y.austin.agent.service.AgentCallLogService;
import com.java3y.austin.common.domain.TaskInfo;
import com.java3y.austin.common.pipeline.BusinessProcess;
import com.java3y.austin.common.pipeline.ProcessContext;
import com.java3y.austin.logistics.context.SentinelContext;
import com.java3y.austin.logistics.dao.WorkorderDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Agent 调用 Action：工单处理（并落库工单）
 * <p>
 * 从 bizId 还原真实订单上下文，构造真实异常描述，调用工单 Agent 分类/定级/生成 SOP，
 * 并把结果写入 workorder 表，打通「异常 → 工单」闭环。
 *
 * @author sentinel
 */
@Service
public class AgentWorkorderAction implements BusinessProcess<TaskInfo> {

    private static final Logger log = LoggerFactory.getLogger(AgentWorkorderAction.class);

    @Autowired
    private WorkorderAgent workorderAgent;
    @Autowired
    private AgentCallLogService agentCallLogService;
    @Autowired
    private WorkorderDao workorderDao;

    @Override
    public void process(ProcessContext<TaskInfo> context) {
        TaskInfo taskInfo = context.getProcessModel();
        String traceId = taskInfo.getBizId() != null ? taskInfo.getBizId() : agentCallLogService.generateTraceId();
        SentinelContext ctx = SentinelContext.fromBizId(taskInfo.getBizId());
        String orderNo = ctx.getOrderNo();

        try {
            String description = "订单在物流环节发生" + SentinelContext.anomalyType(ctx.getNode()) + "异常（节点 " + ctx.getNode() + "）";
            JSONObject result = workorderAgent.process(description, orderNo, traceId);
            log.info("[AgentWorkorderAction] 工单处理完成 orderNo={} type={} level={}",
                    orderNo, result.getString("type"), result.getString("level"));
            if (orderNo != null) {
                workorderDao.insert(orderNo, result.getString("type"), result.getString("level"),
                        description, result.toJSONString(), result.getString("sop"), "OPEN");
                log.info("[AgentWorkorderAction] 工单已落库 orderNo={}", orderNo);
            }
        } catch (Exception e) {
            log.error("[AgentWorkorderAction] Agent 调用异常 orderNo={}", orderNo, e);
        }
    }
}
