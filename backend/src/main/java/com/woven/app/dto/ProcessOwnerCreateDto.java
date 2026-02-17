package com.woven.app.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
/***EVALUATOR - Task B5 - Validation functionality using Jakarta ***/
public record ProcessOwnerCreateDto(
        @NotNull Integer businessProcessOwnerId,
        @NotBlank String name,
        @NotNull Integer positionId

) {}
