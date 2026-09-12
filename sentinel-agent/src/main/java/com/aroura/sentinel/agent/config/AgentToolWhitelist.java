package com.aroura.sentinel.agent.config;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Agent 工具白名单（P0-3）
 * <p>
 * 每个 Agent 只允许装配职责所需的工具；装配时校验，越权直接失败。
 * 当前工具均为只读工具，写操作必须走人工审批与业务接口。
 *
 * @author sentinel
 */
public final class AgentToolWhitelist {

    private AgentToolWhitelist() {
    }

    private static final Map<String, Set<String>> WHITELIST = new HashMap<>();

    static {
        WHITELIST.put("ContentGenAssistant", Collections.singleton("TemplateTool"));
        WHITELIST.put("AnomalyDiagnoseAssistant", Collections.singleton("AnomalyKnowledgeTool"));
        WHITELIST.put("WorkorderAssistant", new HashSet<>(java.util.Arrays.asList("WorkorderTool", "AnomalyKnowledgeTool")));
        WHITELIST.put("CsRouteAssistant", Collections.singleton("LogisticsQueryTool"));
        WHITELIST.put("RouteAdviceAssistant", Collections.singleton("ChannelQuoteTool"));
        WHITELIST.put("EtaPredictAssistant", Collections.singleton("EtaPredictTool"));
    }

    public static Set<String> allowedTools(String assistant) {
        Set<String> allowed = WHITELIST.get(assistant);
        return allowed == null ? Collections.emptySet() : Collections.unmodifiableSet(allowed);
    }

    public static void assertAllowed(String assistant, String toolName) {
        if (assistant == null || toolName == null) {
            throw new IllegalArgumentException("Agent 与工具不能为空");
        }
        Set<String> allowed = WHITELIST.get(assistant);
        if (allowed == null) {
            throw new IllegalArgumentException("未注册的 Agent: " + assistant);
        }
        if (!allowed.contains(toolName)) {
            throw new SecurityException("Agent " + assistant + " 无权装配工具 " + toolName);
        }
    }

    public static void assertAllowed(String assistant, Object tool) {
        if (tool == null) {
            throw new IllegalArgumentException("工具不能为空");
        }
        assertAllowed(assistant, tool.getClass().getSimpleName());
    }
}