package com.woven.app.dto.authorization;

import com.woven.app.domain.Role;

public record MeResponse(

        Integer id,
        String username,
        String fullName,
        Role roles
) { }
