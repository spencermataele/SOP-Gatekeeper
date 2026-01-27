package com.woven.app.web.controller;


import com.woven.app.domain.Sop;
import com.woven.app.dto.SopDto;
import com.woven.app.service.ChangeRequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/change-requests")
@RequiredArgsConstructor
public class ChangeRequestController {

    private final ChangeRequestService changeRequestService;

    @PostMapping("/(id)/publish")
    public ResponseEntity<SopDto> publish(
            @PathVariable Long id,
            @RequestBody SopPublishRequestDto dto,
            Authentication authentication
    ) {
        Sop sop = changeRequestService.publish(id, dto);

        return ResponseEntity.ok(
                SopDto.fromEntity(sop)
        );
    }

    @PostMapping("/approvals/{approvalId}/approve")
    public ResponseEntity<Void> approve(
            @PathVariable Long approvalId,
            @RequestParam Integer userId,
            @RequestParam(required = false) String comments) {

        changeRequestService.approve(approvalId, userId, comments);

        return ResponseEntity.ok().build();
    }

    @PostMapping("/approvals/{approvalId}/reject")
    public ResponseEntity<Void> reject(
            @PathVariable Long approvalId,
            @RequestParam Integer userId,
            @RequestParam String comments) {

        changeRequestService.reject(approvalId, userId, comments);

        return ResponseEntity.ok().build();
    }
}

