package com.aroura.sentinel.web.controller.sentinel;

import com.aroura.sentinel.common.vo.BasicResultVO;
import com.aroura.sentinel.web.annotation.RequireRole;
import com.aroura.sentinel.web.service.sentinel.AgentCopilotService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** Java 智能助手接口：知识问答和运营指标分析。 */
@RestController
@RequestMapping("/api/agent")
@Api(tags = "Sentinel Java 智能助手")
@RequireRole({"ADMIN", "OPERATOR", "CUSTOMER_SERVICE"})
public class SentinelAgentCopilotController {

    private final AgentCopilotService copilotService;

    public SentinelAgentCopilotController(AgentCopilotService copilotService) {
        this.copilotService = copilotService;
    }

    @PostMapping("/chat")
    @ApiOperation("智能问答：知识库检索 + 订单轨迹")
    public BasicResultVO chat(@RequestBody Map<String, Object> body) {
        try {
            return BasicResultVO.success(copilotService.chat(value(body, "question"), value(body, "order_no")));
        } catch (Exception e) {
            return BasicResultVO.fail("智能问答失败：" + e.getMessage());
        }
    }

    @PostMapping("/analytics")
    @RequireRole({"ADMIN", "OPERATOR"})
    @ApiOperation("智能分析：固定只读指标的语义查询")
    public BasicResultVO analytics(@RequestBody Map<String, Object> body) {
        try {
            return BasicResultVO.success(copilotService.analytics(value(body, "question")));
        } catch (Exception e) {
            return BasicResultVO.fail("智能分析失败：" + e.getMessage());
        }
    }

    private String value(Map<String, Object> body, String key) {
        if (body == null) {
            return null;
        }
        Object value = body.get(key);
        return value == null ? null : String.valueOf(value);
    }
}
