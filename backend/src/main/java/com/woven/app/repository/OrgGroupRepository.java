package com.woven.app.repository;

import com.woven.app.domain.Department;
import com.woven.app.domain.OrgGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface OrgGroupRepository extends JpaRepository<OrgGroup, Integer> {
    List<OrgGroup> findByOrg_OrgId(Integer orgId);

    @Query("select distinct g from OrgGroup g left join fetch g.departments")
    List<OrgGroup> findAllWithDepartments();
}
