package com.codeguard.backend.project.dto;

import jakarta.validation.constraints.NotBlank;

public record ProjectRequest(
    @NotBlank(message = "Project name is required") String name,
    @NotBlank(message = "Project description is required") String description
) {
}
