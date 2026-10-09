package com.aroura.sentinel.logistics.dao;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class WorkorderDaoTest {

    @Test
    void updateStatusUsesStatusAndVersionCas() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        WorkorderDao dao = new WorkorderDao(jdbcTemplate);

        String sql = "UPDATE workorder SET status = ?, version = version + 1, updated_at = CURRENT_TIMESTAMP "
                + "WHERE id = ? AND status = ? AND version = ? AND is_deleted = 0";
        when(jdbcTemplate.update(eq(sql), eq("PROCESSING"), eq(1L), eq("OPEN"), eq(3L)))
                .thenReturn(1);

        int updated = dao.updateStatusWithVersion(1L, "OPEN", 3L, "PROCESSING");

        assertEquals(1, updated);
    }
}
