package com.java3y.austin.agent.assistant;

import com.java3y.austin.agent.dto.CsRouteResult;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * 客服路由 Agent 的 AiServices 接口（Tool Calling）
 *
 * @author sentinel
 */
public interface CsRouteAssistant {

    @SystemMessage("你是客服路由专家。识别买家咨询意图，决策「AI 自动回复」还是「转人工」，并生成回复内容。"
            + "涉及物流进度时优先调用 queryTrack 工具查询订单真实轨迹再作答。"
            + "可选意图：query_track/complaint/refund/other；路由：auto/human（投诉、退款建议 human）。"
            + "输出 JSON：{\"intent\":\"意图\",\"route\":\"auto|human\",\"reply\":\"回复内容\"}。")
    @UserMessage("买家咨询：{{message}}；买家ID：{{buyerId}}")
    CsRouteResult route(@V("message") String message,
                        @V("buyerId") String buyerId);
}
