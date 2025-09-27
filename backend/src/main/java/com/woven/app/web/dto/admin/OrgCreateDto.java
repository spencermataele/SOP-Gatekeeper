package com.woven.app.web.dto.admin;

import jakarta.validation.constraints.NotBlank;

public record OrgCreateDto(@NotBlank String orgName) {}
