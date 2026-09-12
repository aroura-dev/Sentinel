package com.aroura.sentinel.service.api.impl.action.send;


import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ReUtil;
import com.alibaba.fastjson2.JSON;
import com.aroura.sentinel.common.domain.TaskInfo;
import com.aroura.sentinel.common.enums.IdType;
import com.aroura.sentinel.common.enums.RespStatusEnum;
import com.aroura.sentinel.common.pipeline.BusinessProcess;
import com.aroura.sentinel.common.pipeline.ProcessContext;
import com.aroura.sentinel.common.vo.BasicResultVO;
import com.aroura.sentinel.service.api.impl.domain.SendTaskModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Sentinel
 * <p>
 * 后置参数检查
 */
@Slf4j
@Service
public class SendAfterCheckAction implements BusinessProcess<SendTaskModel> {

    public static final String PHONE_REGEX_EXP = "^((13[0-9])|(14[5,7,9])|(15[0-3,5-9])|(166)|(17[0-9])|(18[0-9])|(19[1,8,9]))\\d{8}$";
    public static final String EMAIL_REGEX_EXP = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$";
    /**
     * 邮件和手机号正则
     */
    protected static final Map<Integer, String> CHANNEL_REGEX_EXP;

    static {
        Map<Integer, String> tempMap = new HashMap<>();
        tempMap.put(IdType.PHONE.getCode(), PHONE_REGEX_EXP);
        tempMap.put(IdType.EMAIL.getCode(), EMAIL_REGEX_EXP);
        // 初始化为不可变集合，避免被恶意修改
        CHANNEL_REGEX_EXP = Collections.unmodifiableMap(tempMap);
    }


    @Override
    public void process(ProcessContext<SendTaskModel> context) {
        SendTaskModel sendTaskModel = context.getProcessModel();
        List<TaskInfo> taskInfo = sendTaskModel.getTaskInfo();

        // 过滤掉不合法的手机号、邮件
        filterIllegalReceiver(taskInfo);
        if (CollUtil.isEmpty(taskInfo)) {
            context.setNeedBreak(true).setResponse(BasicResultVO.fail(RespStatusEnum.CLIENT_BAD_PARAMETERS, "手机号或邮箱不合法, 无有效的发送任务"));
        }

    }

    /**
     * 如果指定类型是手机号，检测输入手机号是否合法
     * 如果指定类型是邮件，检测输入邮件是否合法
     * 其余 ID 类型（如 userId/openId）不做格式校验，直接放行
     *
     * @param taskInfo
     */
    private void filterIllegalReceiver(List<TaskInfo> taskInfo) {
        Integer idType = CollUtil.getFirst(taskInfo.iterator()).getIdType();
        String regexExp = CHANNEL_REGEX_EXP.get(idType);
        if (regexExp == null) {
            return;
        }
        filter(taskInfo, regexExp);
    }

    /**
     * 利用正则过滤掉不合法的接收者
     *
     * @param taskInfo
     * @param regexExp
     */
    private void filter(List<TaskInfo> taskInfo, String regexExp) {
        Iterator<TaskInfo> iterator = taskInfo.iterator();
        while (iterator.hasNext()) {
            TaskInfo task = iterator.next();
            Set<String> illegalPhone = task.getReceiver().stream()
                    .filter(phone -> !ReUtil.isMatch(regexExp, phone))
                    .collect(Collectors.toSet());

            if (CollUtil.isNotEmpty(illegalPhone)) {
                task.getReceiver().removeAll(illegalPhone);
                log.error("messageTemplateId:{} find illegal receiver!{}", task.getMessageTemplateId(), JSON.toJSONString(illegalPhone));
            }
            if (CollUtil.isEmpty(task.getReceiver())) {
                iterator.remove();
            }
        }
    }

}
