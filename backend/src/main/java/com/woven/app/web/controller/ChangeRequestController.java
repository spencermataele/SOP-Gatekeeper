package com.woven.app.web.controller;

import com.woven.app.domain.Sop;
import com.woven.app.dto.SopDto;
import com.woven.app.dto.SopPublishRequestDto;
import com.woven.app.service.ChangeRequestService;
import com.woven.app.service.user.AppUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/change-requests")
@RequiredArgsConstructor
public class ChangeRequestController {

    private final ChangeRequestService changeRequestService;

    @PostMapping("/{id}/publish")
    public ResponseEntity<SopDto> publish(
            @PathVariable Long id,
            @RequestBody SopPublishRequestDto dto,
            @AuthenticationPrincipal AppUserDetails currentUser
    ) {
        SopDto sop = changeRequestService.publish(id, dto, currentUser);

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
}

