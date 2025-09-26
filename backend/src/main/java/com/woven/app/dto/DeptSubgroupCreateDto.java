package com.woven.app.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record DeptSubgroupCreateDto(
        @NotBlank String deptSubgroupName,
        @NotNull Integer departmentId
) {}
