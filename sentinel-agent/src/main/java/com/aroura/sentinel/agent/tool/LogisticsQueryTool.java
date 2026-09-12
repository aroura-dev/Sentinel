package com.aroura.sentinel.agent.tool;

import com.alibaba.fastjson2.JSON;
import com.aroura.sentinel.logistics.dao.LogisticsDao;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 物流订单/轨迹查询工具（Tool Calling）
 * <p>
 * 供 {@code CsRouteAgent} 调用：识别买家咨询意图时查询订单真实轨迹，
 * 从而判断是「查轨迹」还是「投诉/退款」。
 *
 * @author sentinel
 */
@Component
public class LogisticsQueryTool {

    private final LogisticsDao logisticsDao;

    public LogisticsQueryTool(LogisticsDao logisticsDao) {
        this.logisticsDao = logisticsDao;
    }

    @Tool("查询指定订单的完整物流轨迹（按时间倒序）")
    public String queryTrack(@P("订单号") String orderNo) {
        List<Map<String, Object>> tracks = logisticsDao.listTracks(orderNo);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("orderNo", orderNo);
        result.put("tracks", tracks == null ? 0 : tracks.size());
        result.put("latestNode", tracks == null || tracks.isEmpty() ? "UNKNOWN" : tracks.get(tracks.size() - 1).get("node"));
        return JSON.toJSONString(result);
    }
}
