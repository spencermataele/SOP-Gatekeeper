package com.woven.app.web.dto.admin;

import java.util.List;

public record OrgDto(
        Integer orgId,
        String orgName,
        List<OrgGroupSlimDto> orgGroups
) {}
