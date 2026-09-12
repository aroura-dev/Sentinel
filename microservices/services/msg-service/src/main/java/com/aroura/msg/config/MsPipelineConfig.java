package com.aroura.msg.config;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import com.aroura.sentinel.common.pipeline.ProcessController;
import com.aroura.sentinel.common.pipeline.ProcessTemplate;
import com.aroura.sentinel.handler.action.DeduplicationAction;
import com.aroura.sentinel.handler.action.DiscardAction;
import com.aroura.sentinel.handler.action.SendMessageAction;
import com.aroura.sentinel.handler.action.SensWordsAction;
import com.aroura.sentinel.handler.action.ShieldAction;
import com.aroura.sentinel.handler.config.TaskPipelineConfig;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * msg-service 专用发送管道（替代被排除的 handler 原始 TaskPipelineConfig）。
 * <p>
 * 剔除 AI 相关 3 个 Action 后，普通消息责任链回到纯 sentinel：
 * Discard → Shield → Dedup → SensWords → SendMessage。
 * Task（消费端）只认 {@code handlerProcessController} bean + code=handler，故同名注册。
 *
 * @author sentinel-ms
 */
@Configuration
public class MsPipelineConfig {

    @Autowired
    private DiscardAction discardAction;
    @Autowired
    private ShieldAction shieldAction;
    @Autowired
    private DeduplicationAction deduplicationAction;
    @Autowired
    private SensWordsAction sensWordsAction;
    @Autowired
    private SendMessageAction sendMessageAction;

    @Bean("taskTemplate")
    public ProcessTemplate taskTemplate() {
        ProcessTemplate processTemplate = new ProcessTemplate();
        processTemplate.setProcessList(Arrays.asList(
                discardAction,
                shieldAction,
                deduplicationAction,
                sensWordsAction,
                sendMessageAction
        ));
        return processTemplate;
    }

    @Bean("handlerProcessController")
    public ProcessController handlerProcessController() {
        ProcessController processController = new ProcessController();
        Map<String, ProcessTemplate> templateConfig = new HashMap<>(2);
        templateConfig.put(TaskPipelineConfig.PIPELINE_HANDLER_CODE, taskTemplate());
        processController.setTemplateConfig(templateConfig);
        return processController;
    }
}
