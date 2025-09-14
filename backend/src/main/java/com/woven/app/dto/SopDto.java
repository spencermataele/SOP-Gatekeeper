package com.woven.app.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

/*** Used by the controller to send/receive SOP data without exposing the JPA entity directly. ***/
public record SopDto(

        Integer sopId,

        @NotBlank
        @Size(max = 255)
        String title,

        @NotNull
        Integer authorId,

        @NotNull
        Integer orgId,

        @NotNull
        Integer orgGroupId,

        @NotNull
        Integer departmentId,

        @NotNull
        Integer deptSubgroupId,

        @NotNull
        Integer currentProcessOwnerId,

        @NotNull
        Integer currentProcessOwnerPositionId,

        @NotNull
        Integer processId,

        @NotBlank
        @Size(max = 255)
        String processName,

        @NotNull
        Integer processFamilyId,

        @NotNull
        Integer parentProcessId,

        @NotBlank
        @Size(max = 255)
        String sopLocationPath,

        Instant createdTimestamp,
        Instant updatedTimestamp,

        @NotNull
        Float versionId,

        @NotBlank
        String sopDetails  // LONGTEXT column, holds body text (Markdown/Plain Text)

) {}

