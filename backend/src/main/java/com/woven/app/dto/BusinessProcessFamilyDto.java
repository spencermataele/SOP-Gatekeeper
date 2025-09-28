package com.woven.app.dto;

public record BusinessProcessFamilyDto(
        Integer businessProcessFamilyId,
        String businessProcessFamilyName,
        Integer departmentId,
        String departmentName
) {
}
