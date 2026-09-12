package com.java3y.austin.web.task;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Outbox 重试策略单测（P0-2）
 *
 * @author sentinel
 */
class OutboxEventPublisherTest {

    @Test
    void backoffShouldGrowAndCap() {
        assertEquals(5, OutboxEventPublisher.nextRetrySeconds(0));
        assertEquals(10, OutboxEventPublisher.nextRetrySeconds(1));
        assertEquals(20, OutboxEventPublisher.nextRetrySeconds(2));
        assertEquals(300, OutboxEventPublisher.nextRetrySeconds(6));
        assertEquals(300, OutboxEventPublisher.nextRetrySeconds(99));
    }

    @Test
    void shouldMarkDeadAfterMaxRetry() {
        assertFalse(OutboxEventPublisher.isDead(7));
        assertTrue(OutboxEventPublisher.isDead(8));
    }
}