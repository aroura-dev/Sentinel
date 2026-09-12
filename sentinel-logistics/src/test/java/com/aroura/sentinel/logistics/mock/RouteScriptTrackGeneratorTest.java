package com.aroura.sentinel.logistics.mock;

import com.aroura.sentinel.logistics.enums.LogisticsNode;
import com.aroura.sentinel.logistics.enums.LogisticsNodeTransition;
import com.aroura.sentinel.logistics.model.LogisticsOrder;
import com.aroura.sentinel.logistics.model.LogisticsTrack;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * RouteScriptTrackGenerator 测试：转移合法性、状态码与知识库对齐、驻留时长、终态返回 null
 */
class RouteScriptTrackGeneratorTest {

    private final RouteScriptTrackGenerator generator = new RouteScriptTrackGenerator();

    /** 与 anomaly_knowledge 表对齐的合法状态码集合 */
    private static final Set<String> KNOWLEDGE_STATUS = new HashSet<>(Arrays.asList(
            "EXP-0010", "EXP-0020", "EXP-0030", "EXP-0040", "EXP-0050",
            "EXP-0060", "EXP-0070", "EXP-0080",
            "CUS-1102", "CUS-1103", "CUS-1104", "CUS-1105",
            "EXP-0051", "EXP-0052", "EXP-0062", "EXP-0063",
            "EXP-0071", "EXP-0072", "EXP-0073"
    ));

    private LogisticsOrder order(LogisticsNode node, String country) {
        return LogisticsOrder.builder()
                .orderNo("T-" + node.getCodeEn())
                .buyerId("buyer_test")
                .buyerLanguage("ru")
                .destinationCountry(country)
                .currentNode(node.getCodeEn())
                .build();
    }

    @Test
    void nextTrack_alwaysLegalTransitionWithAlignedStatus() {
        for (LogisticsNode node : LogisticsNode.values()) {
            if (node.isTerminal()) {
                continue;
            }
            LogisticsTrack track = generator.nextTrack(order(node, "RU"));
            assertNotNull(track, "非终态节点 " + node + " 应能推进");

            LogisticsNode next = LogisticsNode.getByCodeEn(track.getNode());
            assertNotNull(next, "目标节点非法: " + track.getNode());
            assertTrue(LogisticsNodeTransition.canTransit(node, next),
                    node.getCodeEn() + " -> " + track.getNode() + " 应为合法转移");
            assertTrue(KNOWLEDGE_STATUS.contains(track.getRawStatus()),
                    node.getCodeEn() + " -> " + track.getNode() + " 状态码 " + track.getRawStatus() + " 未对齐知识库");
            assertNotNull(track.getRawDesc());
            assertNotNull(track.getLocation());
        }
    }

    @Test
    void routeScriptKeyedByCountry_usesDistinctLocations() {
        // RU 订单任意推进，位置不应出现他国独有地点（美/巴/德），且未知目的地回退 RU 主线
        for (LogisticsNode node : activeNodes()) {
            LogisticsTrack t = generator.nextTrack(order(node, "RU"));
            if (t != null) {
                assertFalse(containsForeign(t.getLocation(), "RU"),
                        "RU 订单出现他国独有地点: " + node + " -> " + t.getLocation());
            }
        }
        for (LogisticsNode node : activeNodes()) {
            LogisticsTrack t = generator.nextTrack(order(node, "US"));
            if (t != null) {
                assertFalse(containsForeign(t.getLocation(), "US"),
                        "US 订单出现他国独有地点: " + node + " -> " + t.getLocation());
            }
        }
        LogisticsTrack fallback = generator.nextTrack(order(LogisticsNode.IN_TRANSIT, "ZZ"));
        assertNotNull(fallback);
        assertFalse(containsForeign(fallback.getLocation(), "RU"), "未知目的地应回退 RU 主线: " + fallback.getLocation());
    }

    @Test
    void dwellMillis_positiveForSlowNodes() {
        for (LogisticsNode node : LogisticsNode.values()) {
            if (node.isTerminal()) {
                continue;
            }
            assertTrue(generator.dwellMillis(node) >= 0, node + " 驻留时长不应为负");
        }
        // 国际运输/目的地清关/清关延误为真实长驻留环节
        assertTrue(generator.dwellMillis(LogisticsNode.IN_TRANSIT) > 0);
        assertTrue(generator.dwellMillis(LogisticsNode.IMPORT_CUSTOMS) > 0);
        assertTrue(generator.dwellMillis(LogisticsNode.CUSTOMS_DELAY) > 0);
    }

    private static LogisticsNode[] activeNodes() {
        return Arrays.stream(LogisticsNode.values()).filter(n -> !n.isTerminal()).toArray(LogisticsNode[]::new);
    }

    private static boolean containsForeign(String location, String country) {
        if (location == null) {
            return true;
        }
        if ("RU".equals(country)) {
            return location.contains("Los Angeles") || location.contains("Sao Paulo")
                    || location.contains("Frankfurt") || location.contains("Berlin")
                    || location.contains("Trans-Pacific") || location.contains("South America")
                    || location.contains("Europe flight");
        }
        if ("US".equals(country)) {
            return location.contains("Moscow") || location.contains("Domodedovo")
                    || location.contains("Khorgos") || location.contains("Kazan")
                    || location.contains("Sao Paulo") || location.contains("Frankfurt")
                    || location.contains("Berlin");
        }
        return false;
    }

    @Test
    void terminalNode_returnsNull() {
        for (LogisticsNode terminal : new LogisticsNode[]{LogisticsNode.DELIVERED, LogisticsNode.LOST, LogisticsNode.RETURNED}) {
            assertNull(generator.nextTrack(order(terminal, "RU")), terminal + " 为终态，应返回 null");
        }
    }

}
