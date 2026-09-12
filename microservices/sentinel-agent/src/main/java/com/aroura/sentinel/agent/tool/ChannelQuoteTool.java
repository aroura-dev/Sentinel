package com.aroura.sentinel.agent.tool;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.aroura.sentinel.logistics.dao.tms.CarrierChannelDao;
import com.aroura.sentinel.logistics.engine.FreightCalculator;
import com.aroura.sentinel.logistics.engine.FreightNoRateException;
import com.aroura.sentinel.logistics.model.tms.FreightQuote;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 渠道报价查询工具（真实价卡，非 LLM 编造）
 * <p>
 * 供 {@code RouteAdviceAgent} 调用：目的地 + 重量 → 候选渠道及实时运费，按价格升序。
 *
 * @author sentinel
 */
@Component
public class ChannelQuoteTool {

    private final CarrierChannelDao channelDao;
    private final FreightCalculator freightCalculator;

    public ChannelQuoteTool(CarrierChannelDao channelDao, FreightCalculator freightCalculator) {
        this.channelDao = channelDao;
        this.freightCalculator = freightCalculator;
    }

    @Tool("查询某目的地可用物流渠道及实时运费报价（基于真实价卡），用于智能渠道推荐")
    public String quoteChannels(@P("目的地代码，如 GD/ZJ/JS/SC") String destCountry,
                                @P("货物计费重量（kg）") String weightKg) {
        List<Map<String, Object>> channels = channelDao.listByDestCountry(destCountry);
        BigDecimal weight = new BigDecimal(weightKg);
        JSONArray arr = new JSONArray();
        for (Map<String, Object> ch : channels) {
            try {
                FreightQuote q = freightCalculator.quote(toLong(ch.get("id")), weight, BigDecimal.ZERO, destCountry);
                JSONObject row = new JSONObject();
                row.put("channelId", ch.get("id"));
                row.put("channelCode", ch.get("channel_code"));
                row.put("channelName", ch.get("channel_name"));
                row.put("carrierName", ch.get("carrier_name"));
                row.put("type", ch.get("type"));
                row.put("transitDaysMin", ch.get("transit_days_min"));
                row.put("transitDaysMax", ch.get("transit_days_max"));
                row.put("freight", q.getFreight());
                row.put("currency", q.getCurrency());
                arr.add(row);
            } catch (FreightNoRateException e) {
                // 无价卡渠道跳过
            }
        }
        arr.sort((o1, o2) -> ((JSONObject) o1).getBigDecimal("freight")
                .compareTo(((JSONObject) o2).getBigDecimal("freight")));
        return arr.toJSONString();
    }

    private static Long toLong(Object v) {
        return v == null ? null : Long.valueOf(String.valueOf(v));
    }
}
