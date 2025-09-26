package com.woven.app.repository;

import com.woven.app.domain.Org;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrgRepository extends JpaRepository<Org, Integer> {
}
