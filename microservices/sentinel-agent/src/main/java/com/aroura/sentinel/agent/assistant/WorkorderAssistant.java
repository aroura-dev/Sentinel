package com.aroura.sentinel.agent.assistant;

import com.aroura.sentinel.agent.dto.WorkorderResult;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * 工单处理 Agent 的 AiServices 接口（Tool Calling）
 *
 * @author sentinel
 */
public interface WorkorderAssistant {

    @SystemMessage("你是异常工单处理专家。根据异常描述分类工单、判定优先级、给出处理 SOP。"
            + "优先调用 getExistingWorkorder 工具判断是否已建单，调用 queryKnowledge 工具检索知识库建议。"
            + "可选类型：customs_delay/lost/returned/delivery_failed；级别：P0/P1/P2。"
            + "输出 JSON：{\"type\":\"类型\",\"level\":\"P0|P1|P2\",\"sop\":\"处理步骤\"}。")
    @UserMessage("异常描述：{{anomalyDesc}}；订单信息：{{orderInfo}}")
    WorkorderResult process(@V("anomalyDesc") String anomalyDesc,
                            @V("orderInfo") String orderInfo);
}
