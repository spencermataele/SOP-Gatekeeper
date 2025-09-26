package com.woven.app.repository;

import com.woven.app.domain.Department;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DepartmentRepository extends JpaRepository<Department, Integer> {
    List<Department> findByOrgGroup_OrgGroupId(Integer orgGroupId);
}
