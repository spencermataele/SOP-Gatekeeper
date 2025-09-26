package com.woven.app.repository;

import com.woven.app.domain.OrgGroup;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrgGroupRepository extends JpaRepository<OrgGroup, Integer> {
    List<OrgGroup> findByOrg_OrgId(Integer orgId);
}
