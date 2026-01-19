package com.woven.app.web.dto.admin;

public record UserDto(
        Integer id,
        String username,
        String email,
        // Password not included for security
        String fullName,
        String role
) {}

