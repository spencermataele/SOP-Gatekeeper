package com.woven.app.repository;

import com.woven.app.domain.ProcessOwner;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcessOwnerRepository extends JpaRepository<ProcessOwner, Integer> {
}
