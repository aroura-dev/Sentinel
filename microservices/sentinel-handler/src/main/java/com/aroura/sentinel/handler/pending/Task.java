package com.aroura.sentinel.handler.pending;


import com.aroura.sentinel.common.domain.TaskInfo;
import com.aroura.sentinel.common.pipeline.ProcessContext;
import com.aroura.sentinel.common.pipeline.ProcessController;
import com.aroura.sentinel.common.pipeline.ProcessModel;
import com.aroura.sentinel.common.vo.BasicResultVO;
import com.aroura.sentinel.handler.config.TaskPipelineConfig;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * Task 执行器
 *
 * @author Sentinel
 */
@Data
@Accessors(chain = true)
@Slf4j
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class Task implements Runnable {
    private TaskInfo taskInfo;
    @Autowired
    @Qualifier("handlerProcessController")
    private ProcessController processController;

    /**
     * 本任务处理结束后的回调，用于 Kafka 手动提交 offset。
     * <p>
     * 任务跑在线程池里、而监听器早已返回 —— 消费端因此必须在**任务真正结束**时才提交位移，
     * 否则进程崩溃会丢掉尚未处理的消息。
     */
    private Runnable onCompleted;

    @Override
    public void run() {
        try {
            ProcessContext<ProcessModel> context = ProcessContext.builder()
                    .processModel(taskInfo).code(TaskPipelineConfig.PIPELINE_HANDLER_CODE)
                    .needBreak(false).response(BasicResultVO.success())
                    .build();
            processController.process(context);
        } finally {
            // 必须放在 finally：漏掉一次回调就有一个位移永远提交不了，
            // 会把该分区卡在一条消息上，后续消息全部无法消费。
            if (onCompleted != null) {
                onCompleted.run();
            }
        }
    }
}
