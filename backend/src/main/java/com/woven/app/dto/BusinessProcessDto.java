package com.woven.app.dto;

import java.time.Instant;
import java.util.List;

public record BusinessProcessDto(
        Integer businessProcessId,
        String businessProcessName,
        Integer businessProcessFamily,
        String businessProcessFamilyName,
        Integer parentBusinessProcess,
        String parentProcessName,
        Integer departmentId,
        String departmentName,
        List<Integer> deptSubgroups,
        List<String> deptSubgroupNames,
        Instant lastUpdatedTimestamp
) {}
