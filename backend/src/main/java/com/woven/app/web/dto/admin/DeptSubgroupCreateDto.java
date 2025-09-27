package com.woven.app.web.dto.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record DeptSubgroupCreateDto(
        @NotBlank String deptSubgroupName,
        @NotNull Integer departmentId
) {}
