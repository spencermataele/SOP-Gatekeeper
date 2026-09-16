package com.woven.backend;

import com.woven.app.service.DepartmentService;
import com.woven.app.web.dto.admin.DepartmentDto;
import com.woven.support.DatabaseTest;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@Transactional
class DepartmentServiceIntegrationTest extends DatabaseTest {
    @Autowired DepartmentService departments;
    @Autowired JdbcTemplate jdbc;

    @Test
    void aDepartmentWithoutChildrenStillHasItsParent() {
        Integer parent = jdbc.queryForObject("SELECT MIN(org_group_id) FROM org_group", Integer.class);
        var created = departments.create(new DepartmentDto(null, "Test empty department", parent, List.of()));
        assertEquals(parent, created.orgGroupId());
        assertNotNull(created.subgroups());
        assertTrue(created.subgroups().isEmpty());
        var loaded = departments.listWithSubgroups().stream()
                .filter(item -> item.departmentId().equals(created.departmentId())).findFirst().orElseThrow();
        assertEquals(parent, loaded.orgGroupId());
        assertTrue(loaded.subgroups().isEmpty());
    }

    @Test
    void nonexistentParentCannotCreateADepartment() {
        int before = jdbc.queryForObject("SELECT COUNT(*) FROM department", Integer.class);
        assertThrows(EntityNotFoundException.class, () -> departments.create(
                new DepartmentDto(null, "Invalid parent", Integer.MAX_VALUE, List.of())));
        assertEquals(before, jdbc.queryForObject("SELECT COUNT(*) FROM department", Integer.class));
    }
}
