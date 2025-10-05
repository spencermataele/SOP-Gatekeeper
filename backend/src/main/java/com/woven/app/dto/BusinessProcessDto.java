package com.woven.app.dto;

import java.util.List;

public record BusinessProcessDto(
        Integer businessProcessId,
        String businessProcessName,
        Integer businessProcessFamilyId,
        String businessProcessFamilyName,
        Integer parentBusinessProcessId,
        //String parentProcessName, //Is this needed?
        Integer departmentId,
        //String departmentName,
        List<BusinessProcessSlimDto> businessProcesses
) {}
