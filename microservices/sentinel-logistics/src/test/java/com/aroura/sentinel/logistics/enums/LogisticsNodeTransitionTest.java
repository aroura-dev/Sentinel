package com.aroura.sentinel.logistics.enums;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 物流状态机转移规则：合法转移、非法/回退/终态拒绝
 */
public class LogisticsNodeTransitionTest {

    @Test
    void legalTransitions() {
        assertTrue(LogisticsNodeTransition.canTransit(LogisticsNode.CREATED, LogisticsNode.WAREHOUSE_OUT));
        assertTrue(LogisticsNodeTransition.canTransit(LogisticsNode.IN_TRANSIT, LogisticsNode.IMPORT_CUSTOMS));
        assertTrue(LogisticsNodeTransition.canTransit(LogisticsNode.IN_TRANSIT, LogisticsNode.LOST));
        assertTrue(LogisticsNodeTransition.canTransit(LogisticsNode.IMPORT_CUSTOMS, LogisticsNode.CUSTOMS_DELAY));
        assertTrue(LogisticsNodeTransition.canTransit(LogisticsNode.CUSTOMS_DELAY, LogisticsNode.IMPORT_CUSTOMS));
        assertTrue(LogisticsNodeTransition.canTransit(LogisticsNode.LAST_MILE, LogisticsNode.DELIVERED));
        assertTrue(LogisticsNodeTransition.canTransit(LogisticsNode.LAST_MILE, LogisticsNode.DELIVERY_FAILED));
    }

    @Test
    void illegalTransitionsRejected() {
        assertFalse(LogisticsNodeTransition.canTransit(LogisticsNode.CREATED, LogisticsNode.DELIVERED));
        assertFalse(LogisticsNodeTransition.canTransit(LogisticsNode.CREATED, LogisticsNode.IN_TRANSIT));
        assertFalse(LogisticsNodeTransition.canTransit(LogisticsNode.IN_TRANSIT, LogisticsNode.CREATED));
        assertFalse(LogisticsNodeTransition.canTransit(LogisticsNode.DELIVERED, LogisticsNode.CREATED));
        assertFalse(LogisticsNodeTransition.canTransit(LogisticsNode.LOST, LogisticsNode.IN_TRANSIT));
        assertFalse(LogisticsNodeTransition.canTransit(null, LogisticsNode.CREATED));
    }

    @Test
    void terminalHasNoNext() {
        assertTrue(LogisticsNodeTransition.nextNodes(LogisticsNode.DELIVERED).isEmpty());
        assertTrue(LogisticsNodeTransition.nextNodes(LogisticsNode.LOST).isEmpty());
        assertTrue(LogisticsNodeTransition.nextNodes(LogisticsNode.RETURNED).isEmpty());
    }
}