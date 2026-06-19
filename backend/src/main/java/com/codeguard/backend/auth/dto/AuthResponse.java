package com.codeguard.backend.auth.dto;

import com.codeguard.backend.user.dto.UserResponse;

public record AuthResponse(
    String token,
    UserResponse user
) {
}
