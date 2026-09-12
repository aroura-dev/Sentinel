package com.java3y.austin.handler.config;


import com.java3y.austin.common.pipeline.ProcessController;
import com.java3y.austin.common.pipeline.ProcessTemplate;
import com.java3y.austin.handler.action.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * handler层的pipeline配置类
 * <p>
 * Sentinel 改造：在原有责任链中嵌入 3 个 Agent 调用 Action。
 * 责任链顺序：
 *   0. DiscardAction（复用 austin）
 *   1. ShieldAction（复用 austin）
 *   2. DeduplicationAction（复用 austin）
 *   3. SensWordsAction（复用 austin）
 *   4. AgentContentAction（新增：通知文案生成）
 *   5. SendMessageAction（复用 austin：渠道发送）
 *
 * @author 3y
 */
@Configuration
public class TaskPipelineConfig {
    public static final String PIPELINE_HANDLER_CODE = "handler";
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

    /* ============ Sentinel 新增 Agent 调用 Action ============ */
    @Autowired
    private AgentContentAction agentContentAction;
    @Autowired
    private AgentDiagnoseAction agentDiagnoseAction;
    @Autowired
    private AgentWorkorderAction agentWorkorderAction;

    /**
     * 消息从MQ消费的流程（Sentinel 增强版）
     * 0.丢弃消息（复用 austin）
     * 1.屏蔽消息（复用 austin）
     * 2.通用去重功能（复用 austin）
     * 3.敏感词过滤（复用 austin）
     * 4.文案生成 Agent（新增：买家通知）
     * 5.发送消息（复用 austin）
     *
     * @return
     */
    @Bean("taskTemplate")
    public ProcessTemplate taskTemplate() {
        ProcessTemplate processTemplate = new ProcessTemplate();
        processTemplate.setProcessList(Arrays.asList(
                discardAction,
                shieldAction,
                deduplicationAction,
                sensWordsAction,
                agentContentAction,
                sendMessageAction
        ));
        return processTemplate;
    }

    /**
     * 异常处理责任链（Sentinel 新增）
     * 0.丢弃消息（复用 austin）
     * 1.异常诊断 Agent（新增）
     * 2.文案生成 Agent（新增）
     * 3.工单处理 Agent（新增）
     * 4.发送消息（复用 austin）
     */
    @Bean("anomalyTemplate")
    public ProcessTemplate anomalyTemplate() {
        ProcessTemplate processTemplate = new ProcessTemplate();
        processTemplate.setProcessList(Arrays.asList(
                discardAction,
                agentDiagnoseAction,
                agentContentAction,
                agentWorkorderAction,
                sendMessageAction
        ));
        return processTemplate;
    }

    /**
     * pipeline流程控制器
     * 后续扩展则加BusinessCode和ProcessTemplate
     *
     * @return
     */
    /**
     * 异常处理责任链 code
     */
    public static final String PIPELINE_ANOMALY_CODE = "anomaly";

    @Bean("handlerProcessController")
    public ProcessController processController() {
        ProcessController processController = new ProcessController();
        Map<String, ProcessTemplate> templateConfig = new HashMap<>(4);
        templateConfig.put(PIPELINE_HANDLER_CODE, taskTemplate());
        templateConfig.put(PIPELINE_ANOMALY_CODE, anomalyTemplate());
        processController.setTemplateConfig(templateConfig);
        return processController;
    }
}
