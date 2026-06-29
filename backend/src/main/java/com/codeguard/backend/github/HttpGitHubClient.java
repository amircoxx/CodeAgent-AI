package com.codeguard.backend.github;

import com.codeguard.backend.shared.config.CodeGuardGitHubProperties;
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
import java.util.Map;
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
  public GitHubInstallationMetadata fetchInstallation(Long installationId) {
    JsonNode root = sendGet("/app/installations/" + installationId);
    JsonNode account = root.path("account");
    return new GitHubInstallationMetadata(
        root.path("id").asLong(installationId),
        account.path("login").asText(""),
        account.path("type").asText("")
    );
  }

  @Override
  public GitHubPullRequestMetadata fetchPullRequest(GitHubPullRequestRef pullRequest) {
    return fetchPullRequest(null, pullRequest);
  }

  @Override
  public GitHubPullRequestMetadata fetchPullRequest(
      String installationToken,
      GitHubPullRequestRef pullRequest
  ) {
    JsonNode root = sendGet(pullRequestPath(pullRequest), installationToken);
    String title = root.path("title").asText("");
    String author = root.path("user").path("login").asText("");
    return new GitHubPullRequestMetadata(title, author);
  }

  @Override
  public List<GitHubPullRequestFile> fetchPullRequestFiles(GitHubPullRequestRef pullRequest) {
    return fetchPullRequestFiles(null, pullRequest);
  }

  @Override
  public List<GitHubPullRequestFile> fetchPullRequestFiles(
      String installationToken,
      GitHubPullRequestRef pullRequest
  ) {
    JsonNode root = sendGet(pullRequestPath(pullRequest) + "/files", installationToken);
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

  @Override
  public List<GitHubRepositoryMetadata> listInstallationRepositories(String installationToken) {
    JsonNode root = sendGet("/installation/repositories", installationToken);
    JsonNode repositoriesNode = root.path("repositories");
    if (!repositoriesNode.isArray()) {
      throw new GitHubFetchException("GitHub repositories response was not valid");
    }

    List<GitHubRepositoryMetadata> repositories = new ArrayList<>();
    for (JsonNode repository : repositoriesNode) {
      String fullName = repository.path("full_name").asText("");
      String owner = repository.path("owner").path("login").asText("");
      repositories.add(new GitHubRepositoryMetadata(
          repository.path("id").asLong(),
          owner,
          repository.path("name").asText(""),
          fullName,
          repository.path("private").asBoolean(false)
      ));
    }
    return repositories;
  }

  @Override
  public List<GitHubPullRequestSummary> listPullRequests(
      String installationToken,
      GitHubPullRequestRef pullRequest
  ) {
    JsonNode root = sendGet(
        "/repos/" + pullRequest.owner() + "/" + pullRequest.repo() + "/pulls?state=open",
        installationToken
    );
    if (!root.isArray()) {
      throw new GitHubFetchException("GitHub pull requests response was not valid");
    }

    List<GitHubPullRequestSummary> pullRequests = new ArrayList<>();
    for (JsonNode pullRequestNode : root) {
      pullRequests.add(new GitHubPullRequestSummary(
          pullRequestNode.path("number").asInt(),
          pullRequestNode.path("title").asText(""),
          pullRequestNode.path("user").path("login").asText(""),
          pullRequestNode.path("html_url").asText("")
      ));
    }
    return pullRequests;
  }

  @Override
  public String createPullRequestComment(GitHubPullRequestRef pullRequest, String body) {
    JsonNode root = sendPost(
        issueCommentsPath(pullRequest),
        Map.of("body", body)
    );
    String htmlUrl = root.path("html_url").asText("");
    return htmlUrl.isBlank() ? null : htmlUrl;
  }

  private JsonNode sendGet(String path) {
    return sendGet(path, null);
  }

  private JsonNode sendGet(String path, String bearerToken) {
    try {
      HttpRequest.Builder builder = HttpRequest.newBuilder()
          .uri(URI.create(properties.apiBaseUrl() + path))
          .timeout(Duration.ofSeconds(properties.timeoutSeconds()))
          .header("Accept", "application/vnd.github+json")
          .header("X-GitHub-Api-Version", "2022-11-28")
          .GET();

      String token = bearerToken;
      if ((token == null || token.isBlank()) && properties.hasToken()) {
        token = properties.token();
      }
      if (token != null && !token.isBlank()) {
        builder.header("Authorization", "Bearer " + token);
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

  private JsonNode sendPost(String path, Object payload) {
    try {
      HttpRequest.Builder builder = HttpRequest.newBuilder()
          .uri(URI.create(properties.apiBaseUrl() + path))
          .timeout(Duration.ofSeconds(properties.timeoutSeconds()))
          .header("Accept", "application/vnd.github+json")
          .header("X-GitHub-Api-Version", "2022-11-28")
          .header("Content-Type", "application/json")
          .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload)));

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
      throw new GitHubFetchException("Could not post pull request comment to GitHub", exception);
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      throw new GitHubFetchException("GitHub request was interrupted", exception);
    }
  }

  private String pullRequestPath(GitHubPullRequestRef pullRequest) {
    return "/repos/" + pullRequest.owner() + "/" + pullRequest.repo()
        + "/pulls/" + pullRequest.number();
  }

  private String issueCommentsPath(GitHubPullRequestRef pullRequest) {
    return "/repos/" + pullRequest.owner() + "/" + pullRequest.repo()
        + "/issues/" + pullRequest.number() + "/comments";
  }
}
