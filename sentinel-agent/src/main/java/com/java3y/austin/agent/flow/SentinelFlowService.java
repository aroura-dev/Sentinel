package com.java3y.austin.agent.flow;

import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.flow.LiteflowResponse;
import org.springframework.stereotype.Service;

/**
 * Sentinel 异常处置链执行入口。
 *
 * @author sentinel
 */
@Service
public class SentinelFlowService {

    private final FlowExecutor flowExecutor;

    public SentinelFlowService(FlowExecutor flowExecutor) {
        this.flowExecutor = flowExecutor;
    }

    public SentinelFlowContext execute(SentinelFlowContext context) {
        if (context == null) {
            throw new IllegalArgumentException("流程上下文不能为空");
        }
        LiteflowResponse response = flowExecutor.execute2Resp(
                SentinelFlowConfig.CHAIN_ID, null, context);
        if (!response.isSuccess()) {
            throw new IllegalStateException("异常处置流程执行失败: " + response.getMessage(), response.getCause());
        }
        return response.getFirstContextBean();
    }
}
