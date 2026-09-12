package com.java3y.agent.web;

import java.util.HashMap;
import java.util.Map;

import com.java3y.austin.agent.agent.ContentGenAgent;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * agent-service 内部接口（供 logistics-service 调用；非用户 UI，不走 /api 鉴权）。
 * 生成通知文案并写 agent_call_log（traceId 贯穿，使三库可关联）。
 *
 * @author sentinel-ms
 */
@RestController
@RequestMapping("/internal/agent")
public class AgentController {

    @Autowired
    private ContentGenAgent contentGenAgent;

    /**
     * body: {orderNo, node, language(默认 zh), productInfo, traceId}
     * 返回: {status:"0", data:{content}}
     */
    @PostMapping("/generate")
    public Map<String, Object> generate(@RequestBody Map<String, String> req) {
        String node = req.get("node");
        String language = req.getOrDefault("language", "zh");
        String productInfo = req.getOrDefault("productInfo", "商品信息");
        String orderNo = req.get("orderNo");
        String traceId = req.get("traceId");
        String content = contentGenAgent.generate(node, language, productInfo, orderNo, traceId);

        Map<String, Object> data = new HashMap<>(2);
        data.put("content", content == null ? "" : content);
        Map<String, Object> resp = new HashMap<>(4);
        resp.put("status", "0");
        resp.put("msg", "ok");
        resp.put("data", data);
        return resp;
    }
}
