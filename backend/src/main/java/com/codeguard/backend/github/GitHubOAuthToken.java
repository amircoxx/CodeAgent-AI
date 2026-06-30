package com.codeguard.backend.github;

public record GitHubOAuthToken(String accessToken, String tokenType, String scope) {
}
