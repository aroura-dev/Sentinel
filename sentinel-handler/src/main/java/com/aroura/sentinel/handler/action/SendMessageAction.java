package com.aroura.sentinel.handler.action;

import cn.hutool.core.util.ObjectUtil;
import com.google.common.collect.Sets;
import com.aroura.sentinel.common.domain.TaskInfo;
import com.aroura.sentinel.common.enums.ChannelType;
import com.aroura.sentinel.common.pipeline.BusinessProcess;
import com.aroura.sentinel.common.pipeline.ProcessContext;
import com.aroura.sentinel.handler.handler.HandlerHolder;
import com.aroura.sentinel.logistics.dao.NotificationDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 发送消息，路由到对应的渠道下发消息
 * <p>
 * 同时负责把投递结果回写到 notification_record，闭合「状态机 → 内容生成 → 发送」这条链：
 * {@link AgentContentAction} 在本步骤之前插入一条 PENDING 记录，但此前链路里没有任何组件
 * 会再更新它，导致责任链产生的通知记录全部永久滞留 PENDING，与实际是否送达完全脱钩。
 *
 * @author Sentinel
 */
@Service
public class SendMessageAction implements BusinessProcess<TaskInfo> {

    private static final Logger log = LoggerFactory.getLogger(SendMessageAction.class);

    @Autowired
    private HandlerHolder handlerHolder;
    @Autowired
    private NotificationDao notificationDao;

    @Override
    public void process(ProcessContext<TaskInfo> context) {
        TaskInfo taskInfo = context.getProcessModel();
        // 责任链各组件只能拿到 bizId，它就是 AgentContentAction 插入记录时写入的 trace_id
        String traceId = taskInfo.getBizId();
        try {
            // 微信小程序&服务号只支持单人推送，为了后续逻辑统一处理，于是在这做了单发处理
            if (ChannelType.MINI_PROGRAM.getCode().equals(taskInfo.getSendChannel())
                    || ChannelType.OFFICIAL_ACCOUNT.getCode().equals(taskInfo.getSendChannel())
                    || ChannelType.ALIPAY_MINI_PROGRAM.getCode().equals(taskInfo.getSendChannel())) {
                TaskInfo taskClone = ObjectUtil.cloneByStream(taskInfo);
                for (String receiver : taskInfo.getReceiver()) {
                    taskClone.setReceiver(Sets.newHashSet(receiver));
                    handlerHolder.route(taskInfo.getSendChannel()).doHandler(taskClone);
                }
            } else {
                handlerHolder.route(taskInfo.getSendChannel()).doHandler(taskInfo);
            }
            markOutcome(traceId, "SENT");
        } catch (RuntimeException e) {
            markOutcome(traceId, "FAILED");
            throw e;
        }
    }

    /**
     * 按 trace_id 回写投递结果。
     * <p>
     * 匹配不到记录是正常情况 —— 这条责任链同时也承载普通消息发送，那些消息并没有对应的通知记录，
     * 此时受影响行数为 0。回写本身失败不应影响发送主流程，故只记日志。
     */
    private void markOutcome(String traceId, String status) {
        if (traceId == null || traceId.trim().isEmpty()) {
            return;
        }
        try {
            int updated = notificationDao.updateStatusByTraceId(traceId, status);
            if (updated > 0) {
                log.info("[SendMessageAction] 通知记录已回写 status={} traceId={}", status, traceId);
            }
        } catch (Exception e) {
            log.warn("[SendMessageAction] 通知记录回写失败 status={} traceId={}", status, traceId, e);
        }
    }
}
