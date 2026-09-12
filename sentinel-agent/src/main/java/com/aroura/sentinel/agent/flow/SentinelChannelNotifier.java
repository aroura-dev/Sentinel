package com.aroura.sentinel.agent.flow;

/**
 * 异常处置链到消息触达域的端口，由 Web 层适配现有通知服务。
 *
 * @author sentinel
 */
public interface SentinelChannelNotifier {

    /**
     * @return 本次实际提交/确认的渠道数，已存在 24 小时去重通知时返回 0
     */
    int notifyChannels(String orderNo, String node, String traceId);
}
