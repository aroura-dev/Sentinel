package com.java3y.austin.logistics.mock;

import com.java3y.austin.logistics.enums.LogisticsNode;
import com.java3y.austin.logistics.enums.LogisticsNodeTransition;
import com.java3y.austin.logistics.model.LogisticsOrder;
import com.java3y.austin.logistics.model.LogisticsTrack;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Mock 轨迹生成器：只按状态机合法转移推进，终态返回 null
 */
public class MockTrackGeneratorTest {

    private final MockTrackGenerator generator = new MockTrackGenerator();

    @Test
    void nextTrackIsLegalAndNonTerminal() {
        LogisticsOrder order = LogisticsOrder.builder().orderNo("T1").currentNode("CREATED").build();
        LogisticsTrack track = generator.nextTrack(order);
        assertNotNull(track);
        LogisticsNode next = LogisticsNode.getByCodeEn(track.getNode());
        assertNotNull(next);
        assertTrue(LogisticsNodeTransition.canTransit(LogisticsNode.CREATED, next));
        assertFalse(next.isTerminal());
        assertNotNull(track.getOrderNo());
    }

    @Test
    void terminalReturnsNull() {
        LogisticsOrder delivered = LogisticsOrder.builder().orderNo("T2").currentNode("DELIVERED").build();
        assertNull(generator.nextTrack(delivered));
        LogisticsOrder lost = LogisticsOrder.builder().orderNo("T3").currentNode("LOST").build();
        assertNull(generator.nextTrack(lost));
    }
}