package com.aroura.sentinel.agent.flow;

import com.yomahub.liteflow.core.NodeComponent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * detectException（P1-2 骨架：先跑通编排，后续替换为真实服务调用）
 *
 * @author sentinel
 */
@Slf4j
@Component("detectException")
public class detectException extends NodeComponent {

    @Override
    public void process() throws Exception {
        SentinelFlowContext ctx = getContextBean(SentinelFlowContext.class);
        if (ctx.getAnomalyDesc() == null || ctx.getAnomalyDesc().trim().isEmpty()) {
            throw new IllegalArgumentException("异常描述不能为空");
        }
        log.info("[Flow] detectException orderNo={}", ctx.getOrderNo());
    }
}