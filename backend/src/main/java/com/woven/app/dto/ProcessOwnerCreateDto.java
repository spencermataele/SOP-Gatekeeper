package com.woven.app.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ProcessOwnerCreateDto(
        @NotBlank String name,
        @NotNull Integer positionId,
        Integer parentProcessOwnerId
) {
}
