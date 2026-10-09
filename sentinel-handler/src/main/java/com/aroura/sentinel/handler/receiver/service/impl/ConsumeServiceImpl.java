package com.aroura.sentinel.handler.receiver.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.aroura.sentinel.common.domain.AnchorInfo;
import com.aroura.sentinel.common.domain.LogParam;
import com.aroura.sentinel.common.domain.RecallTaskInfo;
import com.aroura.sentinel.common.domain.TaskInfo;
import com.aroura.sentinel.common.enums.AnchorState;
import com.aroura.sentinel.handler.handler.HandlerHolder;
import com.aroura.sentinel.handler.pending.Task;
import com.aroura.sentinel.handler.pending.TaskPendingHolder;
import com.aroura.sentinel.handler.receiver.service.ConsumeService;
import com.aroura.sentinel.handler.utils.GroupIdMappingUtils;
import com.aroura.sentinel.support.utils.LogUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Sentinel
 */
@Service
public class ConsumeServiceImpl implements ConsumeService {
    private static final String LOG_BIZ_TYPE = "Receiver#consumer";
    private static final String LOG_BIZ_RECALL_TYPE = "Receiver#recall";
    @Autowired
    private ApplicationContext context;

    @Autowired
    private TaskPendingHolder taskPendingHolder;

    @Autowired
    private LogUtils logUtils;

    @Autowired
    private HandlerHolder handlerHolder;

    @Override
    public void consume2Send(List<TaskInfo> taskInfoLists, Acknowledgment ack) {
        if (CollUtil.isEmpty(taskInfoLists)) {
            // 空记录也要提交，否则位移停在原地
            if (ack != null) {
                ack.acknowledge();
            }
            return;
        }
        String topicGroupId = GroupIdMappingUtils.getGroupIdByTaskInfo(CollUtil.getFirst(taskInfoLists.iterator()));
        // 一条记录会拆成 N 个任务并行处理，只有全部结束后才能提交位移。
        // 计数放在 Task 的 finally 里递减，任何一条抛异常也不会漏掉最后一次递减 ——
        // 漏掉就等于位移永远提交不了，该分区会卡死在这一条消息上。
        AtomicInteger pending = new AtomicInteger(taskInfoLists.size());
        for (TaskInfo taskInfo : taskInfoLists) {
            logUtils.print(LogParam.builder().bizType(LOG_BIZ_TYPE).object(taskInfo).build(), AnchorInfo.builder().bizId(taskInfo.getBizId()).messageId(taskInfo.getMessageId()).ids(taskInfo.getReceiver()).businessId(taskInfo.getBusinessId()).state(AnchorState.RECEIVE.getCode()).build());
            Task task = context.getBean(Task.class).setTaskInfo(taskInfo).setOnCompleted(() -> {
                if (pending.decrementAndGet() == 0 && ack != null) {
                    ack.acknowledge();
                }
            });
            taskPendingHolder.route(topicGroupId).execute(task);
        }
    }

    @Override
    public void consume2recall(RecallTaskInfo recallTaskInfo) {
        logUtils.print(LogParam.builder().bizType(LOG_BIZ_RECALL_TYPE).object(recallTaskInfo).build());
        handlerHolder.route(recallTaskInfo.getSendChannel()).recall(recallTaskInfo);
    }
}
