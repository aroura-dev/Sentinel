package com.aroura.sentinel.service.api.impl.action.recall;

import com.google.common.base.Throwables;
import com.aroura.sentinel.common.constant.CommonConstant;
import com.aroura.sentinel.common.domain.RecallTaskInfo;
import com.aroura.sentinel.common.enums.RespStatusEnum;
import com.aroura.sentinel.common.pipeline.BusinessProcess;
import com.aroura.sentinel.common.pipeline.ProcessContext;
import com.aroura.sentinel.common.vo.BasicResultVO;
import com.aroura.sentinel.service.api.impl.domain.RecallTaskModel;
import com.aroura.sentinel.support.dao.MessageTemplateDao;
import com.aroura.sentinel.support.domain.MessageTemplate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * @author Sentinel
 * 组装撤回参数
 */
@Slf4j
@Service
public class RecallAssembleAction implements BusinessProcess<RecallTaskModel> {

    @Autowired
    private MessageTemplateDao messageTemplateDao;

    @Override
    public void process(ProcessContext<RecallTaskModel> context) {
        RecallTaskModel recallTaskModel = context.getProcessModel();
        Long messageTemplateId = recallTaskModel.getMessageTemplateId();
        try {
            Optional<MessageTemplate> messageTemplate = messageTemplateDao.findById(messageTemplateId);
            if (!messageTemplate.isPresent() || messageTemplate.get().getIsDeleted().equals(CommonConstant.TRUE)) {
                context.setNeedBreak(true).setResponse(BasicResultVO.fail(RespStatusEnum.TEMPLATE_NOT_FOUND));
                return;
            }

            RecallTaskInfo recallTaskInfo = RecallTaskInfo.builder().messageTemplateId(messageTemplateId)
                    .recallMessageId(recallTaskModel.getRecallMessageId())
                    .sendAccount(messageTemplate.get().getSendAccount())
                    .sendChannel(messageTemplate.get().getSendChannel())
                    .build();
            recallTaskModel.setRecallTaskInfo(recallTaskInfo);

        } catch (Exception e) {
            context.setNeedBreak(true).setResponse(BasicResultVO.fail(RespStatusEnum.SERVICE_ERROR));
            log.error("assemble recall task fail! templateId:{}, e:{}", messageTemplateId, Throwables.getStackTraceAsString(e));
        }
    }

}
