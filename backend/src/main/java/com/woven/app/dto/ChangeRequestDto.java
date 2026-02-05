package com.woven.app.dto;

import com.woven.app.domain.ChangeStatus;

import java.time.Instant;

public record ChangeRequestDto(

        Long changeRequestId,
        Integer originalSopId,
        Integer requestedByUser,
        String changeSummary,
        String changeReason,
        ChangeStatus changeStatus,
        Instant createdTimestamp,
        Instant updatedTimestamp,
        // To show requested by full name
        String requestByName,
        // To show sop title
        String originalSopTitle,
        // To show current sop version
        String originalSopVersion,
        // new sop id after change published
        Integer publishedSopId

) {}
