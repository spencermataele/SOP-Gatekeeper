package com.woven.app.service;

import com.woven.app.domain.*;
import com.woven.app.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@Transactional
public class ChangeRequestService {

    private final ChangeRequestRepository changeRequestRepository;
    private final ChangeApprovalRepository changeApprovalRepository;
    private final NotificationLogRepository notificationLogRepository;

    private final SopRepository sopRepository;
    private final UserRepository userRepository;

    public ChangeRequestService(
            ChangeRequestRepository changeRequestRepository,
            ChangeApprovalRepository changeApprovalRepository,
            NotificationLogRepository notificationLogRepository,
            SopRepository sopRepository,
            UserRepository userRepository
    ) {
        this.changeRequestRepository = changeRequestRepository;
        this.changeApprovalRepository = changeApprovalRepository;
        this.notificationLogRepository = notificationLogRepository;
        this.sopRepository = sopRepository;
        this.userRepository = userRepository;
    }

    // Create Draft
    public ChangeRequest createDraft(
            Integer sopId,
            Integer requestedByUser,
            String changeSummary,
            String changeReason
    ) {
        Sop sop = sopRepository.findById(sopId).orElseThrow();

        User requester = userRepository.findById(requestedByUser).orElseThrow();

        ChangeRequest changeRequest = new ChangeRequest();
        changeRequest.setSop(sop);
        changeRequest.setRequestedByUser(requester);
        changeRequest.setChangeSummary(changeSummary);
        changeRequest.setChangeReason(changeReason);
        changeRequest.setChangeStatus(ChangeStatus.DRAFT);
        changeRequest.setCreatedTimestamp(Instant.now());
        changeRequest.setUpdatedTimestamp(Instant.now());

        return changeRequestRepository.save(changeRequest);
    }

    // Submit Draft
    public void submitForReview(Long changeRequestId) {
        ChangeRequest changeRequest = changeRequestRepository.findById(changeRequestId).orElseThrow();

        // Prevent duplicate submission of drafts
        if (changeRequest.getChangeStatus() != ChangeStatus.DRAFT) {
            throw new IllegalStateException(
                    "Only new drafts can be submitted"
            );
        }

        changeRequest.setChangeStatus(ChangeStatus.IN_REVIEW);
        changeRequest.setUpdatedTimestamp(Instant.now());

        changeRequestRepository.save(changeRequest);

        createApprovals(changeRequest);
        createNotifications(changeRequest, NotificationType.SUBMITTED);

    }

    // Approve Change Request
    public void approve(
            Long changeApprovalId,
            Integer approver,
            String comments
    ) {
        ChangeApproval approval = changeApprovalRepository.findById(changeApprovalId).orElseThrow();


        if (approval.getApprover().getId() != approver) {
            throw new SecurityException("Wrong approver");
        }

        approval.setDecision(ApprovalDecision.APPROVED);
        approval.setComments(comments);
        //approval.setCreatedTimestamp(Instant.now());
        approval.setUpdatedTimestamp(Instant.now());

        changeApprovalRepository.save(approval);

        // Action for other approvals?
    }

    // Reject Change Request
    public void reject(
            Long changeApprovalId,
            Integer approver,
            String comments
    ) {
        ChangeApproval approval = changeApprovalRepository.findById(changeApprovalId).orElseThrow();

        approval.setDecision(ApprovalDecision.REJECTED);
        approval.setComments(comments);
        //approval.setCreatedTimestamp(Instant.now());
        approval.setUpdatedTimestamp(Instant.now());

        changeApprovalRepository.save(approval);

    }

    // Upon submit, create approvals
    private void createApprovals(ChangeRequest changeRequest) {

        /* For MVP, approver will be process owner for now
        List<ApproverRole> roles = List.of(
                ApproverRole.PROCESS_OWNER,
                ApproverRole.DEPARTMENT_HEAD,
                ApproverRole.QUALITY_ASSURANCE
        );
         */

        Sop sop = changeRequest.getSop();

        Integer currentProcessOwnerId = sop.getCurrent_process_owner_id();

        //Shouldn't happen, but just in case there is no process owner
        if (currentProcessOwnerId == null) {
            throw new IllegalStateException(
                    "SOP has no current process owner"
            );
        }

        User currentProcessOwner = userRepository.findById(currentProcessOwnerId).orElseThrow(()
                -> new IllegalStateException(
                        "Process owner not found: " + currentProcessOwnerId
        ));

        ChangeApproval changeApproval = new ChangeApproval();
        changeApproval.setChangeRequest(changeRequest);
        changeApproval.setApprover(currentProcessOwner);
        changeApproval.setRole(ApproverRole.PROCESS_OWNER);
        changeApproval.setDecision(ApprovalDecision.PENDING);

        changeApprovalRepository.save(changeApproval);

    }

    // Upon submit, create notification
    private void createNotifications(
            ChangeRequest changeRequest,
            NotificationType notificationType
    ) {
        List<User> stakeholders = List.of(changeRequest.getRequestedByUser());

        for (User user : stakeholders) {
            NotificationLog notificationLog = new NotificationLog();

            notificationLog.setChangeRequest(changeRequest);
            notificationLog.setUser(user);
            notificationLog.setCreatedTimestamp(Instant.now());
            notificationLog.setNotificationType(notificationType);

            notificationLogRepository.save(notificationLog);
        }

    }

}
