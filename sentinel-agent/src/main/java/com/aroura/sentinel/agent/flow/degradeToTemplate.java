package com.aroura.sentinel.agent.flow;

import com.yomahub.liteflow.core.NodeComponent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * degradeToTemplate（P1-2 骨架：先跑通编排，后续替换为真实服务调用）
 *
 * @author sentinel
 */
@Slf4j
@Component("degradeToTemplate")
public class degradeToTemplate extends NodeComponent {

    @Override
    public void process() throws Exception {
        SentinelFlowContext ctx = getContextBean(SentinelFlowContext.class);
        ctx.setDegraded(true);
        log.warn("[Flow] 降级处理（不重试模型）orderNo={}", ctx.getOrderNo());
    }
}