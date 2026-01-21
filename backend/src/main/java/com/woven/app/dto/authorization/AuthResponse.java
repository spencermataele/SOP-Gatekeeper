package com.woven.app.dto.authorization;

import com.woven.app.domain.Role;

public record AuthResponse(

        String token,
        Integer id,
        String username,
        String fullName,
        Role roles
) { }
