package com.woven.app.web.dto.admin;

import java.util.List;

public record DepartmentDto(
        Integer departmentId,
        String departmentName,
        Integer orgGroupId,
        List<DeptSubgroupSlimDto> subgroups
) {}
