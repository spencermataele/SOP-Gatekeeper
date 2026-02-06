package com.woven.app.repository;

import com.woven.app.domain.ChangeRequest;
import com.woven.app.domain.ChangeStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChangeRequestRepository extends JpaRepository<ChangeRequest, Long> {

    // Find by status
    List<ChangeRequest> findByChangeStatus(ChangeStatus changeStatus);

    // Find by original SOP id
    List<ChangeRequest> findByOriginalSop_SopId(Integer sopId);

    // Find by proposed SOP id
    List<ChangeRequest> findByProposedSop_SopId(Integer sopId);

    // Find by requested by user
    List<ChangeRequest> findByRequestedByUser_Id(Integer userId);

}
