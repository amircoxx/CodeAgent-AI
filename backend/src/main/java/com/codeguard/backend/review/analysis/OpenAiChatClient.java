package com.codeguard.backend.review.analysis;

import com.codeguard.backend.shared.config.CodeGuardAiProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class OpenAiChatClient implements AiChatClient {

  private final CodeGuardAiProperties properties;
  private final ObjectMapper objectMapper;
  private final HttpClient httpClient;

  public OpenAiChatClient(CodeGuardAiProperties properties, ObjectMapper objectMapper) {
    this.properties = properties;
    this.objectMapper = objectMapper;
    this.httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(properties.timeoutSeconds()))
        .build();
  }

  @Override
  public String completeJson(String prompt) throws IOException, InterruptedException {
    HttpRequest request = HttpRequest.newBuilder()
        .uri(URI.create(properties.endpoint()))
        .timeout(Duration.ofSeconds(properties.timeoutSeconds()))
        .header("Authorization", "Bearer " + properties.apiKey())
        .header("Content-Type", "application/json")
        .POST(HttpRequest.BodyPublishers.ofString(buildRequestBody(prompt)))
        .build();

    HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

    if (response.statusCode() < 200 || response.statusCode() >= 300) {
      throw new IOException("AI provider returned HTTP " + response.statusCode());
    }

    JsonNode root = objectMapper.readTree(response.body());
    JsonNode content = root.path("choices").path(0).path("message").path("content");
    if (!content.isTextual() || content.asText().isBlank()) {
      throw new IOException("AI provider response did not include JSON content");
    }

    return content.asText();
  }

  private String buildRequestBody(String prompt) throws JsonProcessingException {
    Map<String, Object> request = Map.of(
        "model", properties.model(),
        "temperature", 0.2,
        "response_format", Map.of("type", "json_object"),
        "messages", List.of(
            Map.of(
                "role", "system",
                "content", "You are a senior software engineer and security reviewer. Return strict JSON only."
            ),
            Map.of(
                "role", "user",
                "content", prompt
            )
        )
    );

    return objectMapper.writeValueAsString(request);
  }
}
