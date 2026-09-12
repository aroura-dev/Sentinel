package com.java3y.austin.agent.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Agent 工具白名单单测（P0-3）
 *
 * @author sentinel
 */
class AgentToolWhitelistTest {

    @Test
    void whitelistedToolShouldPass() {
        assertDoesNotThrow(() -> AgentToolWhitelist.assertAllowed("CsRouteAssistant", "LogisticsQueryTool"));
        assertDoesNotThrow(() -> AgentToolWhitelist.assertAllowed("WorkorderAssistant", "WorkorderTool"));
    }

    @Test
    void nonWhitelistedToolShouldBeRejected() {
        assertThrows(SecurityException.class,
                () -> AgentToolWhitelist.assertAllowed("CsRouteAssistant", "WorkorderTool"));
        assertThrows(SecurityException.class,
                () -> AgentToolWhitelist.assertAllowed("ContentGenAssistant", "LogisticsQueryTool"));
    }

    @Test
    void unknownAgentShouldBeRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> AgentToolWhitelist.assertAllowed("UnknownAssistant", "TemplateTool"));
    }

    @Test
    void shouldExposeAllowedTools() {
        assertTrue(AgentToolWhitelist.allowedTools("WorkorderAssistant").contains("AnomalyKnowledgeTool"));
    }
}