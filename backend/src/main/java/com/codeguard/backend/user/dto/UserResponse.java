package com.codeguard.backend.user.dto;

public record UserResponse(
    Long id,
    String name,
    String email
) {
}
