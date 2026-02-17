package com.woven.app.service;

import com.woven.app.domain.*;
import com.woven.app.dto.ChangeRequestDto;
import com.woven.app.dto.SopDto;
import com.woven.app.repository.*;
import com.woven.app.service.user.AppUserDetails;
import jakarta.persistence.EntityNotFoundException;
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

    // Start change process by drafting an edited version of existing active sop
    public ChangeRequestDto startChangeDraft(
            Integer originalSop,
            Integer requestedByUser,
            String changeSummary,
            String changeReason
    ) {
        Sop original = sopRepository.findById(originalSop).orElseThrow(
                () -> new EntityNotFoundException("Original SOP not found: " + originalSop)
        );

        if (!Boolean.TRUE.equals(original.getIsActive()) || original.getStatus() != SopStatus.ACTIVE) {
            throw new IllegalStateException("Only ACTIVE SOPs can be edited");
        }

        User requester = userRepository.findById(requestedByUser).orElseThrow(
                () -> new EntityNotFoundException("Requester not found: " + requestedByUser)
        );

        //Clone SOP into the draft to be edited
        Sop changeDraft = cloneForDraft(original);
        changeDraft.setSopId(null);
        changeDraft.setStatus(SopStatus.DRAFT);
        changeDraft.setIsActive(false);

        //Save the clone to add the draft to db
        sopRepository.save(changeDraft);


        ChangeRequest changeRequest = new ChangeRequest();
        changeRequest.setOriginalSop(original);
        changeRequest.setProposedSop(changeDraft);
        changeRequest.setRequestedByUser(requester);
        changeRequest.setChangeSummary(changeSummary);
        changeRequest.setChangeReason(changeReason);
        changeRequest.setChangeStatus(ChangeStatus.DRAFT);

        ChangeRequest saved = changeRequestRepository.save(changeRequest);

        //link new draft to this change request
        changeDraft.setChangeRequest(saved);
        sopRepository.save(changeDraft);

        return toChangeRequestDto(saved);
    }

    // Submit Draft
    public void submitForReview(Long changeRequestId) {
        ChangeRequest changeRequest = changeRequestRepository.findById(changeRequestId).orElseThrow(
                () -> new EntityNotFoundException("ChangeRequest not found: " + changeRequestId)
        );

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

        //Create notification of submitted change request
        createNotifications(changeRequest, NotificationType.SUBMITTED);

    }

    // Approve Change Request
    public void approve(
            Long approvalId,
            Integer approverId,
            String comments,
            AppUserDetails user
    ) {
        System.out.println(">>> APPROVE METHOD VERSION 2 <<<");

        System.out.println("approvalId = " + approvalId);

        System.out.println("currentUser.id = " + approverId);


        ChangeApproval approval = changeApprovalRepository.findById(approvalId).orElseThrow(
                () -> new SecurityException("Approval not found for this user")
        );

        if (approval.getApprover().getId() != approverId) {
            throw new SecurityException("Not your approval");
        }

        if (approval.getDecision() != ApprovalDecision.IN_REVIEW) {
            throw new IllegalStateException("Approval decision is not in REVIEW");
        }

        approval.setDecision(ApprovalDecision.APPROVED);
        approval.setComments(comments);
        approval.setUpdatedTimestamp(Instant.now());

        ChangeRequest changeRequest = approval.getChangeRequest();
        changeRequest.setChangeStatus(ChangeStatus.APPROVED);

        changeApprovalRepository.save(approval);

        // Create notification of approved change request
        createNotifications(changeRequest, NotificationType.APPROVED);

        //Now that it's approved, publish the new SOP
        publish(changeRequest.getChangeRequestId(), user);
    }

    // Publish upon approval
    public SopDto publish(
            Long changeRequestId,
            AppUserDetails user
    ) {
        // Smoke test
        System.out.println(">>> ENTERED publish() for changeRequest " + changeRequestId);

        ChangeRequest changeRequest = changeRequestRepository.findById(changeRequestId).orElseThrow(() ->
                new IllegalArgumentException("ChangeRequest not found " + changeRequestId));

        if (changeRequest.getChangeStatus() != ChangeStatus.APPROVED) {
            throw new IllegalStateException("Only approved changes can be published");
        }

        Sop original = originalSop(changeRequest);
        Sop proposed = draftSop(changeRequest);

        // Make sure correct process owner is the approver
        Integer ownerId = original.getCurrentProcessOwnerId();

        if (!ownerId.equals(user.getUser().getId())) {
            throw new SecurityException("You are not authorized to publish changes to this SOP");
        }

        // Update active flag and status to retired for original
        original.setIsActive(false);
        original.setStatus(SopStatus.RETIRED);
        sopRepository.save(original);

        // Update propsed sop draft to active and isActive
        proposed.setIsActive(true);
        proposed.setStatus(SopStatus.ACTIVE);
        proposed.setPublishedTimestamp(Instant.now());
        sopRepository.save(proposed);

        //Create notification of published change
        createNotifications(changeRequest, NotificationType.PUBLISHED);

        return toDto(proposed);

    }

    // Reject Change Request
    public void reject(
            Long changeApprovalId,
            Integer approver,
            String comments
    ) {
        ChangeApproval approval = changeApprovalRepository.findById(changeApprovalId).orElseThrow(
                () -> new IllegalArgumentException("Approval not found: " + changeApprovalId)
        );

        if (approval.getApprover().getId() != approver) {
            throw new SecurityException("Wrong approver");
        }

        approval.setDecision(ApprovalDecision.REJECTED);
        approval.setComments(comments);
        //approval.setCreatedTimestamp(Instant.now());
        approval.setUpdatedTimestamp(Instant.now());

        changeApprovalRepository.save(approval);

        ChangeRequest changeRequest = approval.getChangeRequest();
        changeRequest.setChangeStatus(ChangeStatus.REJECTED);

        changeRequestRepository.save(changeRequest);

        //Create notification of rejected change
        createNotifications(changeRequest, NotificationType.REJECTED);
    }

    // Cancel Change Request
    public void cancelChangeRequest(Long changeRequestId) {

        ChangeRequest changeRequest = changeRequestRepository.findById(changeRequestId).orElseThrow();

        if (changeRequest.getChangeStatus() != ChangeStatus.DRAFT) {
            throw new IllegalStateException("Only pending change requests can be cancelled");
        }

        Sop draft = sopRepository.findBySopIdAndStatus(
                changeRequest.getProposedSop().getSopId(),
                changeRequest.getProposedSop().getStatus()
        ).orElseThrow();

        if (!draft.getStatus().equals(SopStatus.DRAFT)) {
            throw new IllegalStateException("Only pending change requests can be cancelled");
        }

        changeRequest.setChangeStatus(ChangeStatus.CANCELLED);
        draft.setStatus(SopStatus.CANCELLED);

        sopRepository.save(draft);
        changeRequestRepository.save(changeRequest);
    }

    private Sop originalSop(ChangeRequest changeRequest) {
        if (changeRequest.getOriginalSop() == null) {
            throw new IllegalStateException("ChangeRequest missing original SOP");
        }

        return changeRequest.getOriginalSop();
    }

    private Sop draftSop(ChangeRequest changeRequest) {
        if (changeRequest.getProposedSop() == null) {
            throw new IllegalStateException("ChangeRequest missing draft SOP");
        }

        return changeRequest.getProposedSop();
    }

    private Sop cloneForDraft(Sop original) {
        Sop draft = new Sop();

        draft.setTitle(original.getTitle());
        draft.setAuthorId(original.getAuthorId());
        draft.setOrgId(original.getOrgId());
        draft.setOrgGroupId(original.getOrgGroupId());
        draft.setDepartmentId(original.getDepartmentId());
        draft.setDeptSubgroupId(original.getDeptSubgroupId());
        draft.setCurrentProcessOwnerId(original.getCurrentProcessOwnerId());
        draft.setCurrentProcessOwnerPositionId(original.getCurrentProcessOwnerPositionId());
        draft.setProcessId(original.getProcessId());
        draft.setProcessName(original.getProcessName());
        draft.setProcessFamilyId(original.getProcessFamilyId());
        draft.setParentProcessId(original.getParentProcessId());
        draft.setVersionId(original.getVersionId());
        draft.setSopDescription(original.getSopDescription());
        draft.setSopDetails(original.getSopDetails());
        draft.setIsActive(false);
        draft.setPublishedTimestamp(null);
        draft.setSupersedesSopId(original);
        draft.setVersionId(incrementVersion(original.getVersionId()));

        return draft;
    }

    // Upon submit, create approvals
    private void createApprovals(ChangeRequest changeRequest) {

        Sop original = originalSop(changeRequest);

        Integer currentProcessOwnerId = original.getCurrentProcessOwnerId();

        //Shouldn't happen, but just in case there is no process owner
        if (currentProcessOwnerId == null) {
            throw new IllegalStateException("SOP has no current process owner");
        }

        User currentProcessOwner = userRepository.findById(currentProcessOwnerId).orElseThrow(()
                -> new IllegalStateException("Process owner not found: " + currentProcessOwnerId)
        );

        ChangeApproval changeApproval = new ChangeApproval();
        changeApproval.setChangeRequest(changeRequest);
        changeApproval.setApprover(currentProcessOwner);
        changeApproval.setRole(ApproverRole.PROCESS_OWNER);
        changeApproval.setDecision(ApprovalDecision.IN_REVIEW);

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

    // Because versionId is a String
    private String incrementVersion(String current) {
        try {
            double v = Double.parseDouble(current);
            return String.format("%.1f", v + 0.1);
        } catch (Exception e) {
            return current + ".1";
        }
    }

    private SopDto toDto(Sop sop) {
        return new SopDto(
                sop.getSopId(),
                sop.getTitle(),
                sop.getAuthorId(),
                sop.getOrgId(),
                sop.getOrgGroupId(),
                sop.getDepartmentId(),
                sop.getDeptSubgroupId(),
                sop.getCurrentProcessOwnerId(),
                sop.getCurrentProcessOwnerPositionId(),
                sop.getProcessId(),
                sop.getProcessName(),
                sop.getProcessFamilyId(),
                sop.getParentProcessId(),
                sop.getCreatedTimestamp(),
                sop.getUpdatedTimestamp(),
                sop.getVersionId(),
                sop.getSopDescription(),
                sop.getSopDetails()
        );
    }

    private ChangeRequestDto toChangeRequestDto(ChangeRequest changeRequest) {

        Sop original = changeRequest.getOriginalSop();
        Sop proposed = changeRequest.getProposedSop();

        Integer proposedSopId  = null;

        if (proposed != null) {
            proposedSopId  = proposed.getSopId();
        }

        Long approvalId = changeApprovalRepository
                .findByChangeRequest_ChangeRequestIdAndDecision(
                        changeRequest.getChangeRequestId(),
                        ApprovalDecision.IN_REVIEW
                )
                .stream()
                .map(ChangeApproval::getChangeApprovalId)
                .findFirst()
                .orElse(null);


        return new ChangeRequestDto(
                changeRequest.getChangeRequestId(),
                original != null ? original.getSopId() : null,
                changeRequest.getRequestedByUser().getId(),
                changeRequest.getChangeSummary(),
                changeRequest.getChangeReason(),
                changeRequest.getChangeStatus(),
                changeRequest.getCreatedTimestamp(),
                changeRequest.getUpdatedTimestamp(),
                changeRequest.getRequestedByUser().getFullName(),
                original != null ? original.getTitle() : null,
                original != null ? original.getVersionId(): null,
                proposedSopId,
                approvalId
        );
    }
    /*** EVALUATOR - Task B1 - Polumorphism ***/
    // List all change requests
    @Transactional(readOnly = true)
    public List<ChangeRequestDto> listAll() {
        return changeRequestRepository.findAll().stream().map(this::toChangeRequestDto).toList();
    }

    // List change requests by status
    @Transactional(readOnly = true)
    public List<ChangeRequestDto> listByStatus(ChangeStatus status) {
        return changeRequestRepository
                .findByChangeStatus(status)
                .stream()
                .map(this::toChangeRequestDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public ChangeRequestDto get(Long changeRequestId) {
        return toChangeRequestDto(
                changeRequestRepository.findById(changeRequestId).orElseThrow()
        );
    }

    @Transactional(readOnly = true)
    public List<ChangeRequestDto> findByRequestor(Integer userId) {
        return changeRequestRepository
                .findByRequestedByUser_Id(userId)
                .stream()
                .map(this::toChangeRequestDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ChangeRequestDto> findPendingForApprover(Integer userId) {
        // List only what is pending
        List<ChangeApproval> approvals = changeApprovalRepository
                .findByApprover_IdAndDecision(
                        userId,
                        ApprovalDecision.IN_REVIEW
                );

        return approvals
                .stream()
                .map(ChangeApproval::getChangeRequest)
                .map(this::toChangeRequestDto)
                .toList();

    }

}


