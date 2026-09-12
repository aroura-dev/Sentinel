package com.aroura.sentinel.service.api.impl.domain;

import com.aroura.sentinel.common.domain.RecallTaskInfo;
import com.aroura.sentinel.common.pipeline.ProcessModel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * @author Sentinel
 * @date 2021/11/22
 * @description 发送消息任务模型
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RecallTaskModel implements ProcessModel {

    /**
     * 消息模板Id
     */
    private Long messageTemplateId;

    /**
     * 需要撤回的消息ids
     */
    private List<String> recallMessageId;

    /**
     * 撤回任务 domain
     */
    private RecallTaskInfo recallTaskInfo;
}
