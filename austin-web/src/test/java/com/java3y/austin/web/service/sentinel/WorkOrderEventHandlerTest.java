package com.java3y.austin.web.service.sentinel;

import com.java3y.austin.logistics.dao.ConsumedEventDao;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * 工单事件消费幂等单测（P0-2b）
 *
 * @author sentinel
 */
class WorkOrderEventHandlerTest {

    @Test
    void shouldConsumeOnceAndIgnoreDuplicate() {
        ConsumedEventDao dao = mock(ConsumedEventDao.class);
        when(dao.tryConsume("evt-1", WorkOrderEventHandler.CONSUMER)).thenReturn(true, false);
        WorkOrderEventHandler handler = new WorkOrderEventHandler(dao);
        String msg = "{\"eventId\":\"evt-1\",\"orderNo\":\"SO-1\",\"from\":\"OPEN\",\"to\":\"PROCESSING\"}";

        assertTrue(handler.handle(msg));
        assertFalse(handler.handle(msg));

        verify(dao, times(2)).tryConsume("evt-1", WorkOrderEventHandler.CONSUMER);
    }

    @Test
    void shouldIgnoreMalformedOrMissingEventId() {
        ConsumedEventDao dao = mock(ConsumedEventDao.class);
        WorkOrderEventHandler handler = new WorkOrderEventHandler(dao);

        assertFalse(handler.handle("not-a-json"));
        assertFalse(handler.handle("{\"orderNo\":\"SO-1\"}"));
        verifyNoInteractions(dao);
    }
}