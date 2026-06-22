package com.codeguard.backend.account.dto;

import jakarta.validation.constraints.NotBlank;

public record DeleteAccountRequest(
    @NotBlank(message = "Current password is required") String currentPassword,
    @NotBlank(message = "Delete confirmation is required") String confirmation
) {
}
