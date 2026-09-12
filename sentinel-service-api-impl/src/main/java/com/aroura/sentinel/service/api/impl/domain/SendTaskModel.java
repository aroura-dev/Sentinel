package com.aroura.sentinel.service.api.impl.domain;

import com.aroura.sentinel.common.domain.TaskInfo;
import com.aroura.sentinel.common.pipeline.ProcessModel;
import com.aroura.sentinel.service.api.domain.MessageParam;
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
public class SendTaskModel implements ProcessModel {

    /**
     * 消息模板Id
     */
    private Long messageTemplateId;

    /**
     * 请求参数
     */
    private List<MessageParam> messageParamList;

    /**
     * 发送任务的信息
     */
    private List<TaskInfo> taskInfo;

}
