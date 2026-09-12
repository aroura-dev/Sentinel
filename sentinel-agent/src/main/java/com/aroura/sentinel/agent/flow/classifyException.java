package com.aroura.sentinel.agent.flow;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.aroura.sentinel.agent.agent.WorkorderAgent;
import com.yomahub.liteflow.core.NodeComponent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * classifyException：调用 WorkorderAgent 完成异常分类、优先级与 SOP 生成。
 *
 * @author sentinel
 */
@Slf4j
@Component("classifyException")
public class classifyException extends NodeComponent {

    private final WorkorderAgent workorderAgent;

    public classifyException(WorkorderAgent workorderAgent) {
        this.workorderAgent = workorderAgent;
    }

    @Override
    public void process() {
        SentinelFlowContext ctx = getContextBean(SentinelFlowContext.class);
        JSONObject orderInfo = new JSONObject();
        orderInfo.put("orderNo", ctx.getOrderNo());
        orderInfo.put("logisticsNode", ctx.getLogisticsNode());
        JSONObject result = workorderAgent.process(
                ctx.getAnomalyDesc(), JSON.toJSONString(orderInfo), ctx.getTraceId());

        String type = result == null ? null : result.getString("type");
        String priority = result == null ? null : result.getString("level");
        String sop = result == null ? null : result.getString("sop");
        boolean degraded = result == null || Boolean.TRUE.equals(result.getBoolean("degraded"));
        if (type == null || type.trim().isEmpty()) {
            type = "customs_delay";
            degraded = true;
        }
        if (priority == null || priority.trim().isEmpty()) {
            priority = "P1";
            degraded = true;
        }
        ctx.setExceptionType(type);
        ctx.setPriority(priority);
        ctx.setSop(sop);
        ctx.setLowConfidence(ctx.isLowConfidence() || degraded);
        log.info("[Flow] classifyException orderNo={} type={} priority={} degraded={}",
                ctx.getOrderNo(), type, priority, ctx.isLowConfidence());
    }
}
