package com.codeguard.backend.account.dto;

import jakarta.validation.constraints.NotBlank;

public record PasswordVerificationRequest(
    @NotBlank(message = "Verification code is required") String verificationCode
) {
}
