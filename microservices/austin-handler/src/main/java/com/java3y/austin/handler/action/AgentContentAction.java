package com.java3y.austin.handler.action;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.java3y.austin.agent.agent.ContentGenAgent;
import com.java3y.austin.agent.service.AgentCallLogService;
import com.java3y.austin.common.domain.TaskInfo;
import com.java3y.austin.common.dto.model.ContentModel;
import com.java3y.austin.common.pipeline.BusinessProcess;
import com.java3y.austin.common.pipeline.ProcessContext;
import com.java3y.austin.logistics.context.SentinelContext;
import com.java3y.austin.logistics.dao.NotificationDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Agent 调用 Action：文案生成
 * <p>
 * 职责（合并版）：
 * 1. 从 bizId 还原真实物流上下文（orderNo/node/language/role）；
 * 2. 调用 ContentGenAgent 生成通知文案（模板优先 + AiServices 兜底），回填到 TaskInfo.contentModel；
 * 3. 把文案写入 notification_record，接通「状态机 → 通知」闭环。
 *
 * @author sentinel
 */
@Service
public class AgentContentAction implements BusinessProcess<TaskInfo> {

    private static final Logger log = LoggerFactory.getLogger(AgentContentAction.class);

    @Autowired
    private ContentGenAgent contentGenAgent;
    @Autowired
    private AgentCallLogService agentCallLogService;
    @Autowired
    private NotificationDao notificationDao;

    @Override
    public void process(ProcessContext<TaskInfo> context) {
        TaskInfo taskInfo = context.getProcessModel();
        String traceId = taskInfo.getBizId() != null ? taskInfo.getBizId() : agentCallLogService.generateTraceId();

        SentinelContext ctx = SentinelContext.fromBizId(taskInfo.getBizId());
        try {
            String content = contentGenAgent.generate(ctx.getNode(), ctx.getLanguage(), "商品信息",
                    ctx.getOrderNo(), traceId);
            writeBackContent(taskInfo, content);
            log.info("[AgentContentAction] 文案生成并回填 contentModel 完成 orderNo={} node={} content={}",
                    ctx.getOrderNo(), ctx.getNode(), content);
            if (ctx.getOrderNo() != null) {
                notificationDao.insert(ctx.getOrderNo(), ctx.getNode(), ctx.getRole(), "push",
                        content, ctx.getLanguage(), "PENDING", traceId);
                log.info("[AgentContentAction] 通知记录已落库 orderNo={} node={}", ctx.getOrderNo(), ctx.getNode());
            }
        } catch (Exception e) {
            log.error("[AgentContentAction] Agent 调用异常，沿用原模板文案", e);
        }
    }

    /**
     * 把生成的文案写回 TaskInfo.contentModel（JSON 往返，保持原 ContentModel 类型）
     */
    private void writeBackContent(TaskInfo taskInfo, String content) {
        ContentModel contentModel = taskInfo.getContentModel();
        if (contentModel == null) {
            return;
        }
        JSONObject jsonObject = (JSONObject) JSON.toJSON(contentModel);
        jsonObject.put("content", content);
        taskInfo.setContentModel(JSON.toJavaObject(jsonObject, contentModel.getClass()));
    }
}
