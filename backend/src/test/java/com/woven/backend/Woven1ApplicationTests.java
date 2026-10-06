package com.woven.backend;

import org.junit.jupiter.api.Test;
import com.woven.support.DatabaseTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import static org.junit.jupiter.api.Assertions.*;

class Woven1ApplicationTests extends DatabaseTest {
    @Autowired
    JdbcTemplate jdbc;

    @Test
    void migrationsLoadIntoTheIsolatedTestDatabase() {
        assertEquals("sop_gatekeeper_test", jdbc.queryForObject("SELECT DATABASE()", String.class));
        assertEquals(7, jdbc.queryForObject(
                "SELECT COUNT(*) FROM flyway_schema_history WHERE success = 1", Integer.class));
        assertEquals("107", jdbc.queryForObject(
                "SELECT version FROM flyway_schema_history WHERE success = 1 ORDER BY installed_rank DESC LIMIT 1", String.class));
        assertTrue(jdbc.queryForObject("SELECT COUNT(*) FROM sop", Integer.class) > 0);
    }

    @Test
    void testAccountCannotReadDevelopmentData() {
        assertThrows(org.springframework.dao.DataAccessException.class,
                () -> jdbc.queryForList("SELECT id FROM woven.users LIMIT 1"));
    }

}
