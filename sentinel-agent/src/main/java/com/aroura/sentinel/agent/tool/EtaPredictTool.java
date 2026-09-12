package com.aroura.sentinel.agent.tool;

import com.alibaba.fastjson2.JSONObject;
import com.aroura.sentinel.logistics.dao.LogisticsDao;
import com.aroura.sentinel.logistics.dao.tms.CarrierChannelDao;
import com.aroura.sentinel.logistics.dao.tms.WaybillDao;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.Map;

/**
 * 时效预测工具
 * <p>
 * 供 {@code EtaPredictAgent} 调用：订单当前节点 + 渠道承诺时效 + 已用时间 → 剩余预计送达天数。
 *
 * @author sentinel
 */
@Component
public class EtaPredictTool {

    private final LogisticsDao logisticsDao;
    private final WaybillDao waybillDao;
    private final CarrierChannelDao channelDao;

    public EtaPredictTool(LogisticsDao logisticsDao, WaybillDao waybillDao, CarrierChannelDao channelDao) {
        this.logisticsDao = logisticsDao;
        this.waybillDao = waybillDao;
        this.channelDao = channelDao;
    }

    @Tool("查询订单当前进度与剩余预计送达天数（基于渠道承诺时效与已用时间）")
    public String predictEta(@P("订单号") String orderNo) {
        JSONObject result = new JSONObject();
        result.put("orderNo", orderNo);
        Map<String, Object> order = logisticsDao.findOrderByNo(orderNo);
        if (order == null) {
            result.put("error", "订单不存在");
            return result.toJSONString();
        }
        result.put("node", order.get("current_node"));
        Map<String, Object> wb = waybillDao.findByOrderNo(orderNo);
        if (wb == null) {
            result.put("remainingDays", null);
            result.put("note", "订单未出库，暂无承诺时效");
            return result.toJSONString();
        }
        Map<String, Object> channel = channelDao.findById(toLong(wb.get("channel_id")));
        int transitMax = channel == null ? 10 : toInt(channel.get("transit_days_max"), 10);
        result.put("transitDaysMax", transitMax);
        Object promiseEta = wb.get("promise_eta");
        result.put("promiseEta", promiseEta == null ? null : String.valueOf(promiseEta));
        if (promiseEta instanceof Date) {
            long remainingMs = ((Date) promiseEta).getTime() - System.currentTimeMillis();
            int remainingDays = (int) Math.max(0, Math.ceil(remainingMs / (24.0 * 3600 * 1000)));
            result.put("remainingDays", remainingDays);
            result.put("note", "基于渠道承诺时效估算");
        } else {
            result.put("remainingDays", null);
        }
        return result.toJSONString();
    }

    private static Long toLong(Object v) {
        return v == null ? null : Long.valueOf(String.valueOf(v));
    }

    private static int toInt(Object v, int def) {
        return v == null ? def : Integer.parseInt(String.valueOf(v));
    }
}
