package com.aroura.sentinel.agent.flow;

import com.yomahub.liteflow.core.NodeComponent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * notifyChannels：通过业务端口调用现有通知服务。
 *
 * @author sentinel
 */
@Slf4j
@Component("notifyChannels")
public class notifyChannels extends NodeComponent {

    private final SentinelChannelNotifier channelNotifier;

    public notifyChannels(SentinelChannelNotifier channelNotifier) {
        this.channelNotifier = channelNotifier;
    }

    @Override
    public void process() {
        SentinelFlowContext ctx = getContextBean(SentinelFlowContext.class);
        String node = resolveNode(ctx);
        int count = channelNotifier.notifyChannels(ctx.getOrderNo(), node, ctx.getTraceId());
        ctx.setNotified(true);
        log.info("[Flow] notifyChannels orderNo={} node={} accepted={}",
                ctx.getOrderNo(), node, count);
    }

    private String resolveNode(SentinelFlowContext ctx) {
        if (ctx.getLogisticsNode() != null && !ctx.getLogisticsNode().trim().isEmpty()) {
            return ctx.getLogisticsNode();
        }
        String type = ctx.getExceptionType();
        if (type == null) {
            return "CUSTOMS_DELAY";
        }
        switch (type.toLowerCase()) {
            case "lost":
                return "LOST";
            case "returned":
                return "RETURNED";
            case "delivery_failed":
                return "DELIVERY_FAILED";
            default:
                return "CUSTOMS_DELAY";
        }
    }
}
