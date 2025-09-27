package com.woven.app.web.dto.admin;

import java.util.List;

public record OrgGroupDto(
        Integer orgGroupId,
        String orgGroupName,
        Integer orgId,
        List<DepartmentSlimDto> departments
) {}
