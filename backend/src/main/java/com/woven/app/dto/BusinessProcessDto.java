package com.woven.app.dto;

import java.time.Instant;
import java.util.List;

public record BusinessProcessDto(
        Integer businessProcessId,
        String businessProcessName,
        Integer businessProcessFamilyId,
        String businessProcessFamilyName,
        Integer parentBusinessProcessId,
        String parentProcessName,
        Integer departmentId,
        String departmentName,
        List<Integer> deptSubgroupIds,
        List<String> deptSubgroupNames,
        Instant lastUpdatedTimestamp
) {}
