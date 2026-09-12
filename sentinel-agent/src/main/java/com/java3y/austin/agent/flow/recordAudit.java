package com.java3y.austin.agent.flow;

import com.yomahub.liteflow.core.NodeComponent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * recordAudit（P1-2 骨架：先跑通编排，后续替换为真实服务调用）
 *
 * @author sentinel
 */
@Slf4j
@Component("recordAudit")
public class recordAudit extends NodeComponent {

    @Override
    public void process() throws Exception {
        SentinelFlowContext ctx = getContextBean(SentinelFlowContext.class);
        log.info("[Flow] recordAudit orderNo={} degraded={} notified={}", ctx.getOrderNo(), ctx.isDegraded(), ctx.isNotified());
    }
}