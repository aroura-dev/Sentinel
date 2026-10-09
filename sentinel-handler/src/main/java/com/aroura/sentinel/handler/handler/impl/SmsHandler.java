package com.aroura.sentinel.handler.handler.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.text.CharSequenceUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.google.common.base.Throwables;
import com.aroura.sentinel.common.constant.CommonConstant;
import com.aroura.sentinel.common.domain.NotificationReceipt;
import com.aroura.sentinel.common.domain.RecallTaskInfo;
import com.aroura.sentinel.common.domain.TaskInfo;
import com.aroura.sentinel.common.dto.account.sms.SmsAccount;
import com.aroura.sentinel.common.dto.model.SmsContentModel;
import com.aroura.sentinel.common.enums.ChannelType;
import com.aroura.sentinel.common.enums.SmsStatus;
import com.aroura.sentinel.handler.domain.sms.MessageTypeSmsConfig;
import com.aroura.sentinel.handler.domain.sms.SmsParam;
import com.aroura.sentinel.handler.enums.LoadBalancerStrategy;
import com.aroura.sentinel.handler.handler.BaseHandler;
import com.aroura.sentinel.handler.loadbalance.ServiceLoadBalancerFactory;
import com.aroura.sentinel.handler.script.SmsScript;
import com.aroura.sentinel.support.dao.SmsRecordDao;
import com.aroura.sentinel.support.domain.SmsRecord;
import com.aroura.sentinel.support.service.ConfigService;
import com.aroura.sentinel.support.utils.AccountUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 短信发送处理
 *
 * @author Sentinel
 */
@Component
@Slf4j
public class SmsHandler extends BaseHandler{

    /**
     * 流量自动分配策略
     */
    private static final Integer AUTO_FLOW_RULE = 0;
    private static final String FLOW_KEY = "msgTypeSmsConfig";
    private static final String FLOW_KEY_PREFIX = "message_type_";

    /**
     * 默认负载均衡为随机加权, 待拓展读取配置, 不同Handler可绑定不同的负载均衡策略
     */
    private static final String loadBalancerStrategy = LoadBalancerStrategy.SERVICE_LOAD_BALANCER_RANDOM_WEIGHT_ENHANCED;

    @Autowired
    private SmsRecordDao smsRecordDao;
    @Autowired
    private ConfigService config;
    @Autowired
    private ApplicationContext applicationContext;
    @Autowired
    private AccountUtils accountUtils;
    @Autowired
    private ServiceLoadBalancerFactory<MessageTypeSmsConfig> serviceLoadBalancer;
    /** 用于发布投递回执；共享库不直接依赖任何业务服务的客户端。 */
    @Autowired(required = false)
    private ApplicationEventPublisher eventPublisher;

    public SmsHandler() {
        channelCode = ChannelType.SMS.getCode();
    }

    @Override
    public boolean handler(TaskInfo taskInfo) {
        SmsParam smsParam = SmsParam.builder()
                .phones(taskInfo.getReceiver())
                .content(getSmsContent(taskInfo))
                .messageTemplateId(taskInfo.getMessageTemplateId())
                .build();
        try {
            /**
             * 1、动态配置做流量负载
             * 2、发送短信
             */
            List<MessageTypeSmsConfig> messageTypeSmsConfigs = serviceLoadBalancer.selectService(getMessageTypeSmsConfig(taskInfo), loadBalancerStrategy);
            for (MessageTypeSmsConfig messageTypeSmsConfig : messageTypeSmsConfigs) {
                smsParam.setScriptName(messageTypeSmsConfig.getScriptName());
                smsParam.setSendAccountId(messageTypeSmsConfig.getSendAccount());
                List<SmsRecord> recordList = applicationContext.getBean(messageTypeSmsConfig.getScriptName(), SmsScript.class).send(smsParam);
                if (CollUtil.isNotEmpty(recordList)) {
                    smsRecordDao.saveAll(recordList);
                    publishReceipt(taskInfo, recordList);
                    return true;
                }
            }
        } catch (Exception e) {
            log.error("SmsHandler#handler fail:{},params:{}", Throwables.getStackTraceAsString(e), JSON.toJSONString(smsParam));
        }
        return false;
    }

    /**
     * 发布投递回执。
     * <p>
     * 渠道的真实答复此前只落在 sms_record 里，上游完全看不到 —— logistics 把
     * 「msg-service 受理」直接记成 SENT，于是「已发送」里混着被渠道拒收的短信。
     * <p>
     * 用 Spring 事件而不是直接调 logistics：本类在共享库里，不该依赖某个业务服务的客户端。
     * 没人监听时发布是空操作，不影响发送主链路。
     */
    private void publishReceipt(TaskInfo taskInfo, List<SmsRecord> recordList) {
        if (eventPublisher == null || taskInfo.getBizId() == null) {
            return;
        }
        // 任一接收者被渠道受理即视为已送达 —— 单接收者场景下即为该条的结果
        boolean accepted = recordList.stream()
                .anyMatch(r -> SmsStatus.SEND_SUCCESS.getCode().equals(r.getStatus()));
        eventPublisher.publishEvent(NotificationReceipt.builder()
                .bizId(taskInfo.getBizId())
                .accepted(accepted)
                .detail(accepted ? "渠道已受理" : "渠道拒绝受理，详见 sms_record")
                .build());
    }

    /**
     * 如模板指定具体的明确账号，则优先发其账号，否则走到流量配置
     * <p>
     * 流量配置每种类型都会有其下发渠道账号的配置(流量占比也会配置里面)
     * <p>
     * 样例：
     * key：msgTypeSmsConfig
     * value：[{"message_type_10":[{"weights":80,"scriptName":"TencentSmsScript"},{"weights":20,"scriptName":"YunPianSmsScript"}]},{"message_type_20":[{"weights":20,"scriptName":"YunPianSmsScript"}]},{"message_type_30":[{"weights":20,"scriptName":"TencentSmsScript"}]},{"message_type_40":[{"weights":20,"scriptName":"TencentSmsScript"}]}]
     * 通知类短信有两个发送渠道 TencentSmsScript 占80%流量，YunPianSmsScript占20%流量
     * 营销类短信只有一个发送渠道 YunPianSmsScript
     * 验证码短信只有一个发送渠道 TencentSmsScript
     *
     * @param taskInfo
     * @return
     */
    private List<MessageTypeSmsConfig> getMessageTypeSmsConfig(TaskInfo taskInfo) {

        /**
         * 如果模板指定了账号，则优先使用具体的账号进行发送
         */
        if (!taskInfo.getSendAccount().equals(AUTO_FLOW_RULE)) {
            SmsAccount account = accountUtils.getAccountById(taskInfo.getSendAccount(), SmsAccount.class);
            if (account == null) {
                log.warn("SmsHandler 渠道账号未配置 sendAccount={}", taskInfo.getSendAccount());
                return new ArrayList<>();
            }
            return Collections.singletonList(MessageTypeSmsConfig.builder().sendAccount(taskInfo.getSendAccount()).scriptName(account.getScriptName()).weights(100).build());
        }

        /**
         * 读取流量配置
         */
        String property = config.getProperty(FLOW_KEY, CommonConstant.EMPTY_VALUE_JSON_ARRAY);
        JSONArray jsonArray = JSON.parseArray(property);
        for (int i = 0; i < jsonArray.size(); i++) {
            JSONArray array = jsonArray.getJSONObject(i).getJSONArray(FLOW_KEY_PREFIX + taskInfo.getMsgType());
            if (CollUtil.isNotEmpty(array)) {
                return JSON.parseArray(JSON.toJSONString(array), MessageTypeSmsConfig.class);
            }
        }
        return new ArrayList<>();
    }

    /**
     * 如果有输入链接，则把链接拼在文案后
     * <p>
     * PS: 这里可以考虑将链接 转 短链
     * PS: 如果是营销类的短信，需考虑拼接 回TD退订 之类的文案
     */
    private String getSmsContent(TaskInfo taskInfo) {
        SmsContentModel smsContentModel = (SmsContentModel) taskInfo.getContentModel();
        if (CharSequenceUtil.isNotBlank(smsContentModel.getUrl())) {
            return smsContentModel.getContent() + CharSequenceUtil.SPACE + smsContentModel.getUrl();
        } else {
            return smsContentModel.getContent();
        }
    }

    /**
     * 短信不支持撤回
     * 腾讯云文档 eg：https://cloud.tencent.com/document/product/382/52077
     * @param recallTaskInfo
     */
    @Override
    public void recall(RecallTaskInfo recallTaskInfo) {

    }
}
