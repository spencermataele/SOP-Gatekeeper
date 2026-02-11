package com.woven.app.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record BusinessProcessFamilyCreateDto(
        @NotBlank @Size(max = 255)
        String businessProcessFamilyName,
        @NotNull
        Integer departmentId

) {
}
