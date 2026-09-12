package com.aroura.sentinel.agent.assistant;

import com.aroura.sentinel.agent.dto.RouteAdviceResult;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * 渠道推荐 Agent 的 AiServices 接口（Tool Calling）
 *
 * @author sentinel
 */
public interface RouteAdviceAssistant {

    @SystemMessage("你是国内物流渠道推荐专家。根据目的地和货物重量调用 quoteChannels 获取真实渠道报价与时效，"
            + "权衡价格与时效推荐最优渠道并说明理由。"
            + "输出 JSON：{\"recommendedChannelId\":..,\"channelCode\":\"..\",\"channelName\":\"..\","
            + "\"freight\":..,\"transitDaysMax\":..,\"reason\":\"推荐理由\"}。")
    @UserMessage("目的地：{{destCountry}}；计费重量：{{weightKg}}kg")
    RouteAdviceResult advise(@V("destCountry") String destCountry,
                             @V("weightKg") String weightKg);
}
