package com.woven.app.dto;

import java.time.Instant;
import java.util.List;

public record BusinessProcessFamilyDto(
        Integer businessProcessFamilyId,
        String businessProcessFamilyName,
        Integer departmentId,
        String departmentName,
        Integer deptSubgroupId,
        String deptSubgroupName,
        List<BusinessProcessDto> businessProcesses,
        Instant createdTimestamp,
        Instant lastUpdatedTimestamp
) {
}
