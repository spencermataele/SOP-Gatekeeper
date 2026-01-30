package com.woven.app.repository;

import com.woven.app.domain.ChangeApproval;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChangeApprovalRepository extends JpaRepository<ChangeApproval, Long> {

    // Find by CR id
    List<ChangeApproval> findByChangeRequest_ChangeRequestId(Long changeRequestId);


    boolean existsByChangeRequest_ChangeRequestIdAndApprover_Id(
            Long changeRequestId,
            Integer approverId
    );


}
