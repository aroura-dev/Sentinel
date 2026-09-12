package com.aroura.sentinel.handler.receipt.stater.impl;

import com.aroura.sentinel.common.constant.CommonConstant;
import com.aroura.sentinel.common.enums.ChannelType;
import com.aroura.sentinel.handler.handler.impl.DingDingWorkNoticeHandler;
import com.aroura.sentinel.handler.receipt.stater.ReceiptMessageStater;
import com.aroura.sentinel.support.dao.ChannelAccountDao;
import com.aroura.sentinel.support.domain.ChannelAccount;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

/**
 * 拉取 钉钉工作消息的回执 内容 【未完成】
 *
 * @author Sentinel
 */
public class DingDingWorkReceiptStaterImpl implements ReceiptMessageStater {

    @Autowired
    private DingDingWorkNoticeHandler workNoticeHandler;

    @Autowired
    private ChannelAccountDao channelAccountDao;

    @Override
    public void start() {
        List<ChannelAccount> accountList = channelAccountDao.findAllByIsDeletedEqualsAndSendChannelEquals(CommonConstant.FALSE, ChannelType.DING_DING_WORK_NOTICE.getCode());
        for (ChannelAccount channelAccount : accountList) {
            workNoticeHandler.pull(channelAccount.getId());
        }
    }
}
