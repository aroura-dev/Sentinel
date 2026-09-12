package com.aroura.sentinel.agent.tool;

import com.alibaba.fastjson2.JSON;
import com.aroura.sentinel.logistics.dao.WorkorderDao;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 工单幂等查询工具（Tool Calling）
 * <p>
 * 供 {@code WorkorderAgent} 调用：判断订单是否已存在工单，避免异常链路重复建单。
 *
 * @author sentinel
 */
@Component
public class WorkorderTool {

    private final WorkorderDao workorderDao;

    public WorkorderTool(WorkorderDao workorderDao) {
        this.workorderDao = workorderDao;
    }

    @Tool("查询订单是否已存在工单（用于避免重复建单）")
    public String getExistingWorkorder(@P("订单号") String orderNo) {
        boolean exists = workorderDao.existsByOrderNo(orderNo);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("orderNo", orderNo);
        result.put("exists", exists);
        return JSON.toJSONString(result);
    }
}
