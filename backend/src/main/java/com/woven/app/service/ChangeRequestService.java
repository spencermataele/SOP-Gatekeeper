package com.woven.app.service;

import com.woven.app.domain.*;
import com.woven.app.dto.ChangeRequestDto;
import com.woven.app.dto.SopDto;
import com.woven.app.dto.SopPublishRequestDto;
import com.woven.app.repository.*;
import com.woven.app.service.user.AppUserDetails;
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
        ChangeApproval approval = changeApprovalRepository.findById(changeApprovalId).orElseThrow(
                () ->
                        new IllegalArgumentException(
                                "Approval not found: " + changeApprovalId
                        ));


        if (approval.getApprover().getId() != approver) {
            throw new SecurityException("Wrong approver");
        }

        approval.setDecision(ApprovalDecision.APPROVED);
        approval.setComments(comments);
        //approval.setCreatedTimestamp(Instant.now());
        approval.setUpdatedTimestamp(Instant.now());

        ChangeRequest changeRequest = approval.getChangeRequest();
        changeRequest.setChangeStatus(ChangeStatus.APPROVED);

        changeApprovalRepository.save(approval);
    }

    // Publish upon approval
    public SopDto publish(
            Long id,
            SopPublishRequestDto dto,
            AppUserDetails user
    ) {
        // Smoke test
        System.out.println(">>> ENTERED publish() for changeRequest " + id);

        ChangeRequest changeRequest = changeRequestRepository.findById(id).orElseThrow(() ->
                new IllegalArgumentException("ChangeRequest not found " + id));

        if (changeRequest.getChangeStatus() != ChangeStatus.APPROVED) {
            throw new IllegalStateException(
                    "Only approved changes can be published"
            );
        }

        Sop oldSop = changeRequest.getSop();

        // Make sure correct process owner is the approver
        Integer ownerId = oldSop.getCurrentProcessOwnerId();

        if (!ownerId.equals(user.getUser().getId())) {
            throw new SecurityException("You are not authorized to publish changes to this SOP");
        }

        // Update active flag on old sop
        oldSop.setIsActive(false);
        sopRepository.save(oldSop);

        // Create new SOP version
        Sop newSop = new Sop();

        newSop.setTitle(dto.title());
        newSop.setAuthorId(user.getUser().getId());
        newSop.setOrgId(dto.orgId());
        newSop.setOrgGroupId(dto.orgGroupId());
        newSop.setDepartmentId(dto.departmentId());
        newSop.setDeptSubgroupId(dto.deptSubgroupId());
        newSop.setCurrentProcessOwnerId(dto.currentProcessOwnerId());
        newSop.setCurrentProcessOwnerPositionId(dto.currentProcessOwnerPositionId());
        newSop.setProcessId(dto.processId());
        newSop.setProcessName(dto.processName());
        newSop.setProcessFamilyId(dto.processFamilyId());
        newSop.setParentProcessId(dto.parentProcessId());
        newSop.setSopDescription(dto.sopDescription());
        newSop.setSopDetails(dto.sopDetails());
        newSop.setIsActive(true);
        newSop.setPublishedTimestamp(Instant.now());
        newSop.setChangeRequest(changeRequest);
        newSop.setVersionId(incrementVersion(oldSop.getVersionId()));

        Sop saved = sopRepository.save(newSop);

        // Associate change request with new sop
        changeRequest.setSop(saved);
        changeRequestRepository.save(changeRequest);

        return toDto(saved);

    }

    // Reject Change Request
    public void reject(
            Long changeApprovalId,
            Integer approver,
            String comments
    ) {
        ChangeApproval approval = changeApprovalRepository.findById(changeApprovalId).orElseThrow();

        if (approval.getApprover().getId() != approver) {
            throw new SecurityException("Wrong approver");
        }

        approval.setDecision(ApprovalDecision.REJECTED);
        approval.setComments(comments);
        //approval.setCreatedTimestamp(Instant.now());
        approval.setUpdatedTimestamp(Instant.now());

        ChangeRequest changeRequest = approval.getChangeRequest();
        changeRequest.setChangeStatus(ChangeStatus.REJECTED);

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

        Integer currentProcessOwnerId = sop.getCurrentProcessOwnerId();

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

        // Change request list could include approved changes with new published sop versions
        Integer publishedSopId = null;

        if (changeRequest.getChangeStatus() == ChangeStatus.APPROVED
                && changeRequest.getSop() != null
                && Boolean.TRUE.equals(changeRequest.getSop().getIsActive())) {
            publishedSopId = changeRequest.getSop().getSopId();
        }

        return new ChangeRequestDto(
                changeRequest.getChangeRequestId(),
                changeRequest.getSop().getSopId(),
                changeRequest.getRequestedByUser().getId(),
                changeRequest.getChangeSummary(),
                changeRequest.getChangeReason(),
                changeRequest.getChangeStatus(),
                changeRequest.getCreatedTimestamp(),
                changeRequest.getUpdatedTimestamp(),
                changeRequest.getRequestedByUser().getFullName(),
                changeRequest.getSop().getTitle(),
                changeRequest.getSop().getVersionId(),
                publishedSopId
        );
    }

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


