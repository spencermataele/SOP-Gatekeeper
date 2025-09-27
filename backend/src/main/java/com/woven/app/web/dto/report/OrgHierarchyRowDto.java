package com.woven.app.web.dto.report;

import java.time.Instant;

public record OrgHierarchyRowDto(
        Integer orgId,
        String orgName,
        Integer orgGroupId,
        String orgGroupName,
        Integer departmentId,
        String departmentName,
        Integer deptSubgroupId,
        String deptSubgroupName,
        Instant createdTimestamp,
        Instant lastUpdatedTimestamp
) {}
