package com.woven.app.repository;

import com.woven.app.domain.BusinessProcess;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BusinessProcessRepository extends JpaRepository<BusinessProcess, Integer> {
    List<BusinessProcess> findByBusinessProcessFamily_ProcessFamilyId(Integer processFamilyId);
    List<BusinessProcess> findByParentProcess_ProcessId(Integer parentProcessId);
}
