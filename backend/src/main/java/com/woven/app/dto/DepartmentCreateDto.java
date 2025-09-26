package com.woven.app.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record DepartmentCreateDto(
        @NotBlank String departmentName,
        @NotNull Integer orgGroupId
) {}

