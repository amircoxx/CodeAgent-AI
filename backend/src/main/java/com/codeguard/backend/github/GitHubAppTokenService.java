package com.codeguard.backend.github;

import com.codeguard.backend.shared.config.CodeGuardGitHubProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import org.springframework.stereotype.Service;

@Service
public class GitHubAppTokenService {

  private final CodeGuardGitHubProperties properties;
  private final ObjectMapper objectMapper;
  private final HttpClient httpClient;

  public GitHubAppTokenService(CodeGuardGitHubProperties properties, ObjectMapper objectMapper) {
    this.properties = properties;
    this.objectMapper = objectMapper;
    this.httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(properties.timeoutSeconds()))
        .build();
  }

  public String createInstallationAccessToken(Long installationId) {
    if (properties.appId() == null || properties.appId() <= 0 || isBlank(properties.privateKey())) {
      throw new GitHubFetchException("GitHub App credentials are not configured.");
    }

    try {
      HttpRequest request = HttpRequest.newBuilder()
          .uri(URI.create(properties.apiBaseUrl()
              + "/app/installations/" + installationId + "/access_tokens"))
          .timeout(Duration.ofSeconds(properties.timeoutSeconds()))
          .header("Accept", "application/vnd.github+json")
          .header("X-GitHub-Api-Version", "2022-11-28")
          .header("Authorization", "Bearer " + createAppJwt())
          .POST(HttpRequest.BodyPublishers.noBody())
          .build();
      HttpResponse<String> response = httpClient.send(
          request,
          HttpResponse.BodyHandlers.ofString()
      );

      if (response.statusCode() < 200 || response.statusCode() >= 300) {
        throw new GitHubFetchException("GitHub returned HTTP " + response.statusCode());
      }

      JsonNode root = objectMapper.readTree(response.body());
      String token = root.path("token").asText("");
      if (token.isBlank()) {
        throw new GitHubFetchException("GitHub installation token response was not valid");
      }
      return token;
    } catch (IOException exception) {
      throw new GitHubFetchException("Could not create GitHub installation token", exception);
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      throw new GitHubFetchException("GitHub installation token request was interrupted", exception);
    }
  }

  private String createAppJwt() {
    try {
      String header = "{\"alg\":\"RS256\",\"typ\":\"JWT\"}";
      Instant now = Instant.now();
      String payload = "{\"iat\":" + now.minusSeconds(60).getEpochSecond()
          + ",\"exp\":" + now.plusSeconds(540).getEpochSecond()
          + ",\"iss\":\"" + properties.appId() + "\"}";
      String signingInput = base64Url(header.getBytes(StandardCharsets.UTF_8))
          + "." + base64Url(payload.getBytes(StandardCharsets.UTF_8));

      Signature signature = Signature.getInstance("SHA256withRSA");
      signature.initSign(parsePrivateKey(properties.privateKey()));
      signature.update(signingInput.getBytes(StandardCharsets.UTF_8));
      return signingInput + "." + base64Url(signature.sign());
    } catch (Exception exception) {
      throw new GitHubFetchException("Could not sign GitHub App JWT", exception);
    }
  }

  private PrivateKey parsePrivateKey(String rawPrivateKey) throws Exception {
    String normalized = rawPrivateKey
        .replace("\\n", "\n")
        .replace("-----BEGIN PRIVATE KEY-----", "")
        .replace("-----END PRIVATE KEY-----", "")
        .replaceAll("\\s", "");
    byte[] decoded = Base64.getDecoder().decode(normalized);
    return KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(decoded));
  }

  private String base64Url(byte[] bytes) {
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
  }

  private boolean isBlank(String value) {
    return value == null || value.isBlank();
  }
}
