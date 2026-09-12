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

    @Override
    public void run() {
        ProcessContext<ProcessModel> context = ProcessContext.builder()
                .processModel(taskInfo).code(TaskPipelineConfig.PIPELINE_HANDLER_CODE)
                .needBreak(false).response(BasicResultVO.success())
                .build();
        processController.process(context);
    }
}
