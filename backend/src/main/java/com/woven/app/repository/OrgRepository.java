package com.woven.app.repository;

import com.woven.app.domain.Org;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface OrgRepository extends JpaRepository<Org, Integer> {

    @Query("select distinct o from Org o left join fetch o.orgGroups")
    List<Org> findAllWithGroups();
}
