package com.java3y.austin.agent.flow;

import com.yomahub.liteflow.core.NodeBooleanComponent;
import org.springframework.stereotype.Component;

/**
 * 低置信度条件节点。
 *
 * @author sentinel
 */
@Component("isLowConfidence")
public class isLowConfidence extends NodeBooleanComponent {

    @Override
    public boolean processBoolean() {
        SentinelFlowContext ctx = getContextBean(SentinelFlowContext.class);
        return ctx.isLowConfidence();
    }
}
