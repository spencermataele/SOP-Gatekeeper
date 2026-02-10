package com.woven.app.web.controller;

import com.woven.app.domain.ChangeRequest;
import com.woven.app.domain.ChangeStatus;
import com.woven.app.dto.ChangeRequestDto;
import com.woven.app.dto.SopDto;
import com.woven.app.dto.SopPublishRequestDto;
import com.woven.app.service.ChangeRequestService;
import com.woven.app.service.user.AppUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/change-requests")
@RequiredArgsConstructor
public class ChangeRequestController {

    private final ChangeRequestService changeRequestService;

    @PostMapping("/{id}/publish")
    public ResponseEntity<SopDto> publish(
            @PathVariable Long id,
            @AuthenticationPrincipal AppUserDetails currentUser
    ) {
        SopDto sop = changeRequestService.publish(id, currentUser);

        return ResponseEntity.ok(sop);
    }

    @PostMapping("/approvals/{approvalId}/approve")
    public ResponseEntity<Void> approve(
            @PathVariable Long approvalId,
            @RequestParam(required = false) String comments,
            @AuthenticationPrincipal AppUserDetails currentUser) {

        changeRequestService.approve(
                approvalId,
                currentUser.getUser().getId(),
                comments
        );

        return ResponseEntity.ok().build();
    }

    @PostMapping("/approvals/{approvalId}/reject")
    public ResponseEntity<Void> reject(
            @PathVariable Long approvalId,
            @RequestParam String comments,
            @AuthenticationPrincipal AppUserDetails currentUser) {

        changeRequestService.reject(
                approvalId,
                currentUser.getUser().getId(),
                comments
        );

        return ResponseEntity.ok().build();
    }

    @PostMapping("/start")
    public ResponseEntity<ChangeRequestDto> startChangeDraft(
            @RequestParam Integer originalSopId,
            @AuthenticationPrincipal AppUserDetails currentUser,
            @RequestParam String summary,
            @RequestParam String reason
    ) {
        ChangeRequestDto changeRequest = changeRequestService.startChangeDraft(
                originalSopId,
                currentUser.getUser().getId(),
                summary,
                reason
        );

        return ResponseEntity.ok(changeRequest);
    }

    @PostMapping("/{id}/submit")
    public ResponseEntity<Void> submit(@PathVariable Long id) {
        changeRequestService.submitForReview(id);

        return ResponseEntity.ok().build();
    }

    @GetMapping
    public List<ChangeRequestDto> list(
        @RequestParam(required = false)ChangeStatus status
    ) {
        if (status != null) {
            return changeRequestService.listByStatus(status);
        }

        return changeRequestService.listAll();

    }

    @GetMapping("/{id}")
    public ChangeRequestDto get(@PathVariable Long id) {
        return changeRequestService.get(id);
    }

    // For "my requests" notifications
    @GetMapping("/mine")
    public List<ChangeRequestDto> myRequests(
          @AuthenticationPrincipal AppUserDetails user
    ) {
        return changeRequestService
                .findByRequestor(user.getUser().getId());
    }

    // For "my pending approvals" notifications
    @GetMapping("/pending-approval")
    public List<ChangeRequestDto> pendingApprovals(
            @AuthenticationPrincipal AppUserDetails user
    ) {
        return changeRequestService
                .findPendingForApprover(user.getUser().getId());
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<Void> cancel(@PathVariable Long id) {
        changeRequestService.cancelChangeRequest(id);

        return ResponseEntity.noContent().build();
    }

}

