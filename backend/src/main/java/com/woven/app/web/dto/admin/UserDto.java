package com.woven.app.web.dto.admin;

public record UserDto(
        Integer id,
        String username,
        String email,
        String password,
        String fullName,
        String roles
) {}
