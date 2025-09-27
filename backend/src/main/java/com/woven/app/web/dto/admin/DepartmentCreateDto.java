package com.woven.app.web.dto.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record DepartmentCreateDto(
        @NotBlank String departmentName,
        @NotNull Integer orgGroupId
) {}

