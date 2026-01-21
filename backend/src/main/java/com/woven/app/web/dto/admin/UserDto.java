package com.woven.app.web.dto.admin;

import com.woven.app.domain.Role;

public record UserDto(
        Integer id,
        String username,
        String email,
        // Password not included for security
        String fullName,
        Role role
) {}

