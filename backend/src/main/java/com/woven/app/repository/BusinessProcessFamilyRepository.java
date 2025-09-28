package com.woven.app.repository;

import com.woven.app.domain.BusinessProcessFamily;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BusinessProcessFamilyRepository extends JpaRepository<BusinessProcessFamily,Integer> {
    List<BusinessProcessFamily> findByDepartment_DepartmentId(Integer departmentId);
}
