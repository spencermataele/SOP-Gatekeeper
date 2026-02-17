package com.woven.app.repository;

import com.woven.app.domain.Sop;
import com.woven.app.domain.SopStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;


public interface SopRepository extends JpaRepository<Sop, Integer> {

        List<Sop> findByStatus(SopStatus currentStatus);

    // For creating change request draft
    Optional<Sop> findBySopIdAndStatus(Integer sopId, SopStatus currentStatus);

}
