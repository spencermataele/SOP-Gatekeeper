package com.woven.app.dto.authorization;

import com.woven.app.domain.Role;

import java.util.Set;

public record MeResponse(

        Integer id,
        String username,
        String fullName,
        Set<Role> roles
) { }
