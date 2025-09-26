package com.woven.app.dto;

import com.woven.app.domain.ProcessOwner;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ProcessOwnerCreateDto(
        @NotBlank String name,
        @NotNull Integer positionId
        //, ProcessOwner parentProcessOwnerID
) {}
