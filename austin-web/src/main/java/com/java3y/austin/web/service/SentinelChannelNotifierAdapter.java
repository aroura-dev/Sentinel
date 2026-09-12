package com.java3y.austin.web.service;

import com.java3y.austin.agent.flow.SentinelChannelNotifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * 把 Agent 编排端口适配到现有 Sentinel 通知发送链路。
 *
 * @author sentinel
 */
@Service
public class SentinelChannelNotifierAdapter implements SentinelChannelNotifier {

    private final SentinelNotifyService notifyService;

    @Value("${sentinel.flow.notify-channel:sms}")
    private String channel;

    public SentinelChannelNotifierAdapter(SentinelNotifyService notifyService) {
        this.notifyService = notifyService;
    }

    @Override
    public int notifyChannels(String orderNo, String node, String traceId) {
        Map<String, Object> row = notifyService.send(orderNo, node, "buyer", channel, false, traceId);
        return row == null ? 0 : 1;
    }
}
