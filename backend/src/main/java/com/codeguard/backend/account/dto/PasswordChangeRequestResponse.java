package com.codeguard.backend.account.dto;

public record PasswordChangeRequestResponse(
    String message,
    String devVerificationCode
) {
}
