package com.java3y.austin.agent.assistant;

import com.java3y.austin.agent.dto.AnomalyDiagnosis;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * 异常诊断 Agent 的 AiServices 接口（Tool Calling + 知识库 RAG）
 *
 * @author sentinel
 */
public interface AnomalyDiagnoseAssistant {

    @SystemMessage("你是国内物流异常诊断专家。根据异常类型、状态码、订单信息，诊断异常原因并给出处理建议。"
            + "优先调用 queryKnowledge 工具按状态码检索异常知识库，结合知识库内容作答。"
            + "输出 JSON：{\"reason\":\"原因\",\"suggestion\":\"建议\",\"priority\":\"P0|P1|P2\"}，priority 只能取 P0/P1/P2。")
    @UserMessage("异常类型：{{anomalyType}}；状态码：{{statusCode}}；订单信息：{{orderInfo}}")
    AnomalyDiagnosis diagnose(@V("anomalyType") String anomalyType,
                              @V("statusCode") String statusCode,
                              @V("orderInfo") String orderInfo);
}
