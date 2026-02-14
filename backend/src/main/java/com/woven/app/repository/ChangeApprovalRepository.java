package com.woven.app.repository;

import com.woven.app.domain.ApprovalDecision;
import com.woven.app.domain.ChangeApproval;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ChangeApprovalRepository extends JpaRepository<ChangeApproval, Long> {

    // Find by change request id
    List<ChangeApproval> findByChangeRequest_ChangeRequestId(Long changeRequestId);


    List<ChangeApproval> findByChangeRequest_ChangeRequestIdAndDecision(
            Long changeRequestId,
            ApprovalDecision decision
    );

    // Find by approver and decision (for pending approval notification)
    List<ChangeApproval> findByApprover_IdAndDecision(
            Integer approverId,
            ApprovalDecision decision
    );

}
