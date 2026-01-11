package com.woven.app.repository;

import com.woven.app.domain.BusinessProcess;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface BusinessProcessRepository extends JpaRepository<BusinessProcess, Integer> {
    @Query("""
      select distinct p
      from BusinessProcess p
      left join fetch p.parentBusinessProcess par
      join fetch p.businessProcessFamily pf
      join fetch pf.department d
      left join fetch d.orgGroup og
      left join fetch og.org o
      left join fetch p.deptSubgroups sg
      where (:orgId is null or (o is not null and o.orgId = :orgId))
        and (:orgGroupId is null or (og is not null and og.orgGroupId = :orgGroupId))
        and (:departmentId is null or d.departmentId = :departmentId)
        and (:businessProcessFamilyId is null or pf.businessProcessFamilyId = :businessProcessFamilyId)
        and (:search is null or lower(p.processName) like concat('%', lower(:search), '%'))
      """)
    List<BusinessProcess> findForProcessStreamReport(Integer orgId,
                                                     Integer orgGroupId,
                                                     Integer departmentId,
                                                     Integer businessProcessFamilyId,
                                                     String search);
}
