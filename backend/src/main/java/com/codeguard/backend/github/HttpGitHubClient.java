package com.codeguard.backend.github;

import com.codeguard.backend.config.CodeGuardGitHubProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class HttpGitHubClient implements GitHubClient {

  private final CodeGuardGitHubProperties properties;
  private final ObjectMapper objectMapper;
  private final HttpClient httpClient;

  public HttpGitHubClient(CodeGuardGitHubProperties properties, ObjectMapper objectMapper) {
    this.properties = properties;
    this.objectMapper = objectMapper;
    this.httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(properties.timeoutSeconds()))
        .build();
  }

  @Override
  public GitHubPullRequestMetadata fetchPullRequest(GitHubPullRequestRef pullRequest) {
    JsonNode root = sendGet(pullRequestPath(pullRequest));
    String title = root.path("title").asText("");
    String author = root.path("user").path("login").asText("");
    return new GitHubPullRequestMetadata(title, author);
  }

  @Override
  public List<GitHubPullRequestFile> fetchPullRequestFiles(GitHubPullRequestRef pullRequest) {
    JsonNode root = sendGet(pullRequestPath(pullRequest) + "/files");
    if (!root.isArray()) {
      throw new GitHubFetchException("GitHub files response was not valid");
    }

    List<GitHubPullRequestFile> files = new ArrayList<>();
    for (JsonNode file : root) {
      files.add(new GitHubPullRequestFile(
          file.path("filename").asText(""),
          file.path("status").asText("modified"),
          file.path("patch").asText("")
      ));
    }
    return files;
  }

  private JsonNode sendGet(String path) {
    try {
      HttpRequest.Builder builder = HttpRequest.newBuilder()
          .uri(URI.create(properties.apiBaseUrl() + path))
          .timeout(Duration.ofSeconds(properties.timeoutSeconds()))
          .header("Accept", "application/vnd.github+json")
          .header("X-GitHub-Api-Version", "2022-11-28")
          .GET();

      if (properties.hasToken()) {
        builder.header("Authorization", "Bearer " + properties.token());
      }

      HttpResponse<String> response = httpClient.send(
          builder.build(),
          HttpResponse.BodyHandlers.ofString()
      );

      if (response.statusCode() < 200 || response.statusCode() >= 300) {
        throw new GitHubFetchException("GitHub returned HTTP " + response.statusCode());
      }

      return objectMapper.readTree(response.body());
    } catch (IOException exception) {
      throw new GitHubFetchException("Could not fetch pull request data from GitHub", exception);
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      throw new GitHubFetchException("GitHub request was interrupted", exception);
    }
  }

  private String pullRequestPath(GitHubPullRequestRef pullRequest) {
    return "/repos/" + pullRequest.owner() + "/" + pullRequest.repo()
        + "/pulls/" + pullRequest.number();
  }
}
