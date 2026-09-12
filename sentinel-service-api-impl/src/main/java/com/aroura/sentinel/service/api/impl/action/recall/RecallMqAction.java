package com.aroura.sentinel.service.api.impl.action.recall;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONWriter;
import com.google.common.base.Throwables;
import com.aroura.sentinel.common.domain.RecallTaskInfo;
import com.aroura.sentinel.common.enums.RespStatusEnum;
import com.aroura.sentinel.common.pipeline.BusinessProcess;
import com.aroura.sentinel.common.pipeline.ProcessContext;
import com.aroura.sentinel.common.vo.BasicResultVO;
import com.aroura.sentinel.service.api.impl.domain.RecallTaskModel;
import com.aroura.sentinel.support.mq.SendMqService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * @author Sentinel
 * 将撤回消息发送到MQ
 */
@Slf4j
@Service
public class RecallMqAction implements BusinessProcess<RecallTaskModel> {
    @Autowired
    private SendMqService sendMqService;

    @Value("${sentinel.business.recall.topic.name}")
    private String sentinelRecall;
    @Value("${sentinel.business.tagId.value}")
    private String tagId;

    @Value("${sentinel.mq.pipeline}")
    private String mqPipeline;

    @Override
    public void process(ProcessContext<RecallTaskModel> context) {
        RecallTaskInfo recallTaskInfo = context.getProcessModel().getRecallTaskInfo();
        try {
            String message = JSON.toJSONString(recallTaskInfo, JSONWriter.Feature.WriteClassName);
            sendMqService.send(sentinelRecall, message, tagId);
        } catch (Exception e) {
            context.setNeedBreak(true).setResponse(BasicResultVO.fail(RespStatusEnum.SERVICE_ERROR));
            log.error("send {} fail! e:{},params:{}", mqPipeline, Throwables.getStackTraceAsString(e)
                    , JSON.toJSONString(recallTaskInfo));
        }
    }

}
