package com.woven.app.repository;

import com.woven.app.domain.DeptSubgroup;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DeptSubgroupRepository extends JpaRepository<DeptSubgroup, Integer> {
    List<DeptSubgroup> findByDepartment_DepartmentId(Integer departmentId);
}
