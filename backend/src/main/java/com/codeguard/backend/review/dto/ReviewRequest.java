package com.codeguard.backend.review.dto;

import jakarta.validation.constraints.NotBlank;

public record ReviewRequest(
    @NotBlank(message = "Language is required") String language,
    @NotBlank(message = "Code is required") String code
) {
}
