package com.woven.app.repository;

import com.woven.app.domain.Sop;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SopRepository extends JpaRepository<Sop, Integer> {

    // For sop list, show only active sops
    // TODO: Depreciate isActive
    List<Sop> findByIsActiveTrue();

    List<Sop> findByStatus();

}
