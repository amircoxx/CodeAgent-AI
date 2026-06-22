package com.codeguard.backend.account.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CompletePasswordChangeRequest(
    @NotBlank(message = "Verification code is required") String verificationCode,
    @NotBlank(message = "New password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    String newPassword
) {
}
