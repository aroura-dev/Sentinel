package com.java3y.austin.agent.flow;

import com.alibaba.fastjson2.JSONObject;
import com.java3y.austin.logistics.dao.WorkorderDao;
import com.yomahub.liteflow.core.NodeComponent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * createWorkorder：幂等创建异常工单并回填 workOrderId。
 *
 * @author sentinel
 */
@Slf4j
@Component("createWorkorder")
public class createWorkorder extends NodeComponent {

    private final WorkorderDao workorderDao;

    public createWorkorder(WorkorderDao workorderDao) {
        this.workorderDao = workorderDao;
    }

    @Override
    public void process() {
        SentinelFlowContext ctx = getContextBean(SentinelFlowContext.class);
        if (ctx.getWorkOrderId() != null) {
            return;
        }
        Long existingId = workorderDao.findLatestIdByOrderNo(ctx.getOrderNo());
        if (existingId != null) {
            ctx.setWorkOrderId(existingId);
            log.info("[Flow] createWorkorder 复用已有工单 orderNo={} workOrderId={}",
                    ctx.getOrderNo(), existingId);
            return;
        }

        String type = ctx.getExceptionType() == null ? "customs_delay" : ctx.getExceptionType();
        String level = ctx.getPriority() == null ? "P1" : ctx.getPriority();
        JSONObject diagnosis = new JSONObject();
        diagnosis.put("type", type);
        diagnosis.put("level", level);
        diagnosis.put("sop", ctx.getSop());
        diagnosis.put("traceId", ctx.getTraceId());
        diagnosis.put("degraded", ctx.isLowConfidence());

        Long id = workorderDao.insertAndReturnId(
                ctx.getOrderNo(), type, level, ctx.getAnomalyDesc(),
                diagnosis.toJSONString(), ctx.getSop(), "OPEN");
        ctx.setWorkOrderId(id);
        log.info("[Flow] createWorkorder orderNo={} workOrderId={} type={}",
                ctx.getOrderNo(), id, type);
    }
}
