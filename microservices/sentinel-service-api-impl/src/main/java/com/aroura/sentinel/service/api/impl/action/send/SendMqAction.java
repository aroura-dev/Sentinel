package com.aroura.sentinel.service.api.impl.action.send;

import cn.hutool.core.collection.CollUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONWriter;
import com.google.common.base.Throwables;
import com.aroura.sentinel.common.domain.SimpleTaskInfo;
import com.aroura.sentinel.common.domain.TaskInfo;
import com.aroura.sentinel.common.enums.RespStatusEnum;
import com.aroura.sentinel.common.pipeline.BusinessProcess;
import com.aroura.sentinel.common.pipeline.ProcessContext;
import com.aroura.sentinel.common.vo.BasicResultVO;
import com.aroura.sentinel.service.api.impl.domain.SendTaskModel;
import com.aroura.sentinel.support.mq.SendMqService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Sentinel
 * 1. 将消息发送到MQ
 * 2. 返回拼装好的messageId给到接口调用方
 */
@Slf4j
@Service
public class SendMqAction implements BusinessProcess<SendTaskModel> {


    @Autowired
    private SendMqService sendMqService;

    @Value("${sentinel.business.topic.name}")
    private String sendMessageTopic;

    @Value("${sentinel.business.tagId.value}")
    private String tagId;

    @Value("${sentinel.mq.pipeline}")
    private String mqPipeline;

    @Override
    public void process(ProcessContext<SendTaskModel> context) {
        SendTaskModel sendTaskModel = context.getProcessModel();
        List<TaskInfo> taskInfo = sendTaskModel.getTaskInfo();
        try {
            String message = JSON.toJSONString(sendTaskModel.getTaskInfo(), JSONWriter.Feature.WriteClassName);
            sendMqService.send(sendMessageTopic, message, tagId);

            context.setResponse(BasicResultVO.success(taskInfo.stream().map(v -> SimpleTaskInfo.builder().businessId(v.getBusinessId()).messageId(v.getMessageId()).bizId(v.getBizId()).build()).collect(Collectors.toList())));
        } catch (Exception e) {
            context.setNeedBreak(true).setResponse(BasicResultVO.fail(RespStatusEnum.SERVICE_ERROR));
            log.error("send {} fail! e:{},params:{}", mqPipeline, Throwables.getStackTraceAsString(e)
                    , JSON.toJSONString(CollUtil.getFirst(taskInfo.listIterator())));
        }
    }

}
