package com.java3y.austin.logistics.enums;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 工单状态机转移规则单测
 *
 * @author sentinel
 */
class WorkOrderStateTransitionTest {

    @Test
    void shouldAllowNormalTransitions() {
        assertTrue(WorkOrderStateTransition.canTransit(WorkOrderState.OPEN, WorkOrderState.PROCESSING));
        assertTrue(WorkOrderStateTransition.canTransit(WorkOrderState.PROCESSING, WorkOrderState.PUSHED));
        assertTrue(WorkOrderStateTransition.canTransit(WorkOrderState.PUSHED, WorkOrderState.RESOLVED));
        assertTrue(WorkOrderStateTransition.canTransit(WorkOrderState.RESOLVED, WorkOrderState.CLOSED));
    }

    @Test
    void shouldRejectIllegalTransitions() {
        assertFalse(WorkOrderStateTransition.canTransit(WorkOrderState.CLOSED, WorkOrderState.OPEN));
        assertFalse(WorkOrderStateTransition.canTransit(WorkOrderState.RESOLVED, WorkOrderState.OPEN));
        assertFalse(WorkOrderStateTransition.canTransit(WorkOrderState.OPEN, WorkOrderState.OPEN));
        assertFalse(WorkOrderStateTransition.canTransit(null, WorkOrderState.OPEN));
    }

    @Test
    void shouldThrowWithReadableMessage() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> WorkOrderStateTransition.requireValid(WorkOrderState.CLOSED, WorkOrderState.OPEN));
        assertTrue(ex.getMessage().contains("非法状态迁移"));
    }

    @Test
    void terminalStateHasNoNextStates() {
        Set<WorkOrderState> next = WorkOrderStateTransition.nextStates(WorkOrderState.CLOSED);
        assertTrue(next.isEmpty());
        assertTrue(WorkOrderState.CLOSED.isTerminal());
        assertFalse(WorkOrderState.OPEN.isTerminal());
    }

    @Test
    void shouldParseStateCode() {
        assertEquals(WorkOrderState.PROCESSING, WorkOrderState.fromCode("processing"));
        assertEquals(WorkOrderState.OPEN, WorkOrderState.fromCode(" OPEN "));
        assertNull(WorkOrderState.fromCode("UNKNOWN"));
        assertNull(WorkOrderState.fromCode(null));
    }
}