package com.aroura.sentinel.logistics.enums;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 物流节点枚举：终态/异常标记、编码查找
 */
public class LogisticsNodeTest {

    @Test
    void terminalFlags() {
        assertTrue(LogisticsNode.DELIVERED.isTerminal());
        assertTrue(LogisticsNode.LOST.isTerminal());
        assertTrue(LogisticsNode.RETURNED.isTerminal());
        assertFalse(LogisticsNode.IN_TRANSIT.isTerminal());
        assertFalse(LogisticsNode.CUSTOMS_DELAY.isTerminal());
    }

    @Test
    void anomalyFlags() {
        assertTrue(LogisticsNode.CUSTOMS_DELAY.isAnomaly());
        assertTrue(LogisticsNode.DELIVERY_FAILED.isAnomaly());
        assertTrue(LogisticsNode.LOST.isAnomaly());
        assertTrue(LogisticsNode.RETURNED.isAnomaly());
        assertFalse(LogisticsNode.DELIVERED.isAnomaly());
        assertFalse(LogisticsNode.IN_TRANSIT.isAnomaly());
    }

    @Test
    void getByCode() {
        assertEquals(LogisticsNode.CREATED, LogisticsNode.getByCode(10));
        assertEquals(LogisticsNode.IN_TRANSIT, LogisticsNode.getByCodeEn("IN_TRANSIT"));
        assertNull(LogisticsNode.getByCode(999));
        assertNull(LogisticsNode.getByCodeEn("UNKNOWN"));
    }
}