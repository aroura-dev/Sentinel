package com.java3y.austin.agent.assistant;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * 文案生成 Agent 的 AiServices 接口
 * <p>
 * 模板优先由 {@code TemplateTool} 承担；本接口负责在模板未命中时兜底生成通知文案。
 *
 * @author sentinel
 */
public interface ContentGenAssistant {

    @SystemMessage("你是国内物流通知文案生成专家。根据物流节点、买家语言、商品信息生成一条自然、贴心、不超过 100 字的通知文案。"
            + "如果存在与「物流节点+语言」匹配的模板，优先调用 queryTemplate 工具获取并复用模板；"
            + "否则基于商品信息个性化生成。只输出文案本身，不要输出解释或 Markdown。")
    @UserMessage("请用 {{language}} 语言生成通知文案。物流节点：{{node}}；商品信息：{{productInfo}}；订单号：{{orderNo}}")
    String generate(@V("node") String node,
                    @V("language") String language,
                    @V("productInfo") String productInfo,
                    @V("orderNo") String orderNo);
}
