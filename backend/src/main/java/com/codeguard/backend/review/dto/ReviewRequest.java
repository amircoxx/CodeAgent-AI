package com.codeguard.backend.review.dto;

import jakarta.validation.constraints.NotBlank;

public record ReviewRequest(
    Long projectId,
    @NotBlank(message = "Title is required") String title,
    @NotBlank(message = "Language is required") String language,
    @NotBlank(message = "Code is required") String code
) {
}
