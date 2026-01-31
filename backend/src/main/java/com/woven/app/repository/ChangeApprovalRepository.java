package com.woven.app.repository;

import com.woven.app.domain.ApprovalDecision;
import com.woven.app.domain.ChangeApproval;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChangeApprovalRepository extends JpaRepository<ChangeApproval, Long> {

    // Find by change request id
    List<ChangeApproval> findByChangeRequest_ChangeRequestId(Long changeRequestId);


    boolean existsByChangeRequest_ChangeRequestIdAndApprover_Id(
            Long changeRequestId,
            Integer approverId
    );

    // Find by approver and decision (for pending approval notification)
    List<ChangeApproval> findByApprover_IdAndDecision(
            Integer approverId,
            ApprovalDecision decision
    );

}
