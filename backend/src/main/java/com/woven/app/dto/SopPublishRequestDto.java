package com.woven.app.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SopPublishRequestDto(

        @NotBlank
        String title,

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
        String processName,

        @NotNull
        Integer processFamilyId,

        Integer parentProcessId,

        @NotBlank
        String sopDescription,

        @NotBlank
        String sopDetails

) {}
