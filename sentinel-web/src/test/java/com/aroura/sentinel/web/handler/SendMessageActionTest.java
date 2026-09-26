package com.aroura.sentinel.web.handler;

import com.aroura.sentinel.common.domain.TaskInfo;
import com.aroura.sentinel.common.pipeline.ProcessContext;
import com.aroura.sentinel.handler.action.SendMessageAction;
import com.aroura.sentinel.handler.handler.Handler;
import com.aroura.sentinel.handler.handler.HandlerHolder;
import com.aroura.sentinel.logistics.dao.NotificationDao;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 验证发送责任链会把投递结果回写到 notification_record。
 * <p>
 * 放在 sentinel-web 的测试树里是因为 sentinel-handler 模块没有测试依赖，
 * 而 sentinel-web 通过 handler → agent → logistics 传递依赖能看到这些类。
 * <p>
 * 此前链路里没有任何组件更新那条 PENDING 记录，责任链产生的通知记录会永久滞留，
 * 这里正对着那个回归。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SendMessageActionTest {

    private static final String TRACE_ID = "OMT-SEED-0028|CUSTOMS_DELAY|zh|buyer";

    @Mock
    private HandlerHolder handlerHolder;
    @Mock
    private NotificationDao notificationDao;
    @Mock
    private Handler handler;

    @InjectMocks
    private SendMessageAction action;

    private ProcessContext<TaskInfo> context() {
        TaskInfo taskInfo = new TaskInfo();
        taskInfo.setBizId(TRACE_ID);
        taskInfo.setSendChannel(30);
        ProcessContext<TaskInfo> context = new ProcessContext<>();
        context.setProcessModel(taskInfo);
        return context;
    }

    @Test
    void dispatchSucceeds_marksRecordSent() {
        when(handlerHolder.route(30)).thenReturn(handler);

        action.process(context());

        verify(handler).doHandler(any(TaskInfo.class));
        verify(notificationDao).updateStatusByTraceId(TRACE_ID, "SENT");
    }

    @Test
    void dispatchThrows_marksRecordFailedAndPropagates() {
        when(handlerHolder.route(30)).thenReturn(handler);
        doThrow(new IllegalStateException("渠道不可用")).when(handler).doHandler(any(TaskInfo.class));

        assertThrows(IllegalStateException.class, () -> action.process(context()));

        // 发送失败必须回写 FAILED，且异常仍要向责任链上抛（保持原有传播语义）
        verify(notificationDao).updateStatusByTraceId(TRACE_ID, "FAILED");
    }

    @Test
    void recordWriteBackFailure_doesNotBreakDispatch() {
        when(handlerHolder.route(30)).thenReturn(handler);
        when(notificationDao.updateStatusByTraceId(TRACE_ID, "SENT"))
                .thenThrow(new RuntimeException("db down"));

        // 回写失败不应影响发送主流程
        action.process(context());

        verify(handler).doHandler(any(TaskInfo.class));
    }
}
