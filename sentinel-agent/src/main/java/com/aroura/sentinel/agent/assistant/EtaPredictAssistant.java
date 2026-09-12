package com.aroura.sentinel.agent.assistant;

import com.aroura.sentinel.agent.dto.EtaPredictResult;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * 时效预测 Agent 的 AiServices 接口（Tool Calling）
 *
 * @author sentinel
 */
public interface EtaPredictAssistant {

    @SystemMessage("你是物流时效预测专家。调用 predictEta 获取订单当前进度与承诺时效，估算剩余送达天数。"
            + "输出 JSON：{\"orderNo\":..,\"remainingDays\":..,\"etaDate\":\"yyyy-MM-dd\",\"confidence\":\"high|medium|low\",\"reason\":\"依据\"}。")
    @UserMessage("订单号：{{orderNo}}")
    EtaPredictResult predict(@V("orderNo") String orderNo);
}
