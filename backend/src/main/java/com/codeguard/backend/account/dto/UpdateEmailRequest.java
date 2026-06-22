package com.codeguard.backend.account.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record UpdateEmailRequest(
    @Email(message = "Email must be valid") @NotBlank(message = "Email is required") String email,
    @NotBlank(message = "Current password is required") String currentPassword
) {
}
