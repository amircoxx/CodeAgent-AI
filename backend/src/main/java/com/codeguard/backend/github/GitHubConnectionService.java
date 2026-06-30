package com.codeguard.backend.github;

import com.codeguard.backend.auth.CurrentUserService;
import com.codeguard.backend.github.dto.GitHubConnectUrlResponse;
import com.codeguard.backend.github.dto.GitHubConnectionResponse;
import com.codeguard.backend.github.entity.GitHubConnectionEntity;
import com.codeguard.backend.github.entity.GitHubPendingConnectionEntity;
import com.codeguard.backend.github.repository.GitHubConnectionRepository;
import com.codeguard.backend.github.repository.GitHubPendingConnectionRepository;
import com.codeguard.backend.shared.config.CodeGuardGitHubProperties;
import com.codeguard.backend.user.entity.UserEntity;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GitHubConnectionService {

  private final CurrentUserService currentUserService;
  private final GitHubConnectionRepository gitHubConnectionRepository;
  private final GitHubPendingConnectionRepository gitHubPendingConnectionRepository;
  private final GitHubClient gitHubClient;
  private final CodeGuardGitHubProperties properties;

  public GitHubConnectionService(
      CurrentUserService currentUserService,
      GitHubConnectionRepository gitHubConnectionRepository,
      GitHubPendingConnectionRepository gitHubPendingConnectionRepository,
      GitHubClient gitHubClient,
      CodeGuardGitHubProperties properties
  ) {
    this.currentUserService = currentUserService;
    this.gitHubConnectionRepository = gitHubConnectionRepository;
    this.gitHubPendingConnectionRepository = gitHubPendingConnectionRepository;
    this.gitHubClient = gitHubClient;
    this.properties = properties;
  }

  @Transactional(readOnly = true)
  public GitHubConnectionResponse getConnection() {
    UserEntity user = currentUserService.getCurrentUser();
    return gitHubConnectionRepository.findByUserId(user.getId())
        .map(this::toResponse)
        .orElseGet(GitHubConnectionResponse::disconnected);
  }

  @Transactional
  public GitHubConnectUrlResponse createConnectUrl() {
    UserEntity user = currentUserService.getCurrentUser();
    Instant now = Instant.now();
    gitHubPendingConnectionRepository.deleteAllByExpiresAtBefore(now);
    gitHubPendingConnectionRepository.deleteAllByUserId(user.getId());

    String state = UUID.randomUUID().toString();
    gitHubPendingConnectionRepository.save(new GitHubPendingConnectionEntity(
        user,
        state,
        now.plus(properties.pendingStateTtlMinutes(), ChronoUnit.MINUTES)
    ));

    if (!properties.hasOAuthCredentials()) {
      throw new GitHubFetchException("GitHub OAuth credentials are not configured.");
    }

    String connectUrl = properties.oauthAuthorizeUrl()
        + "?client_id=" + encode(properties.oauthClientId())
        + "&redirect_uri=" + encode(properties.oauthCallbackUrl())
        + "&scope=" + encode(properties.resolvedOAuthScope())
        + "&state=" + encode(state);
    return new GitHubConnectUrlResponse(connectUrl, state);
  }

  @Transactional
  public String completeSetup(String code, String state) {
    if (code == null || code.isBlank()) {
      throw new GitHubSetupStateException("GitHub OAuth code is required.");
    }

    UserEntity user = currentUserService.getCurrentUser();
    GitHubPendingConnectionEntity pending = validatePendingState(user, state);
    GitHubOAuthToken token = gitHubClient.exchangeOAuthCode(code.trim());
    GitHubAuthenticatedUser gitHubUser = gitHubClient.fetchAuthenticatedUser(token.accessToken());
    GitHubConnectionEntity connection = gitHubConnectionRepository.findByUserId(user.getId())
        .orElseGet(() -> new GitHubConnectionEntity(
            user,
            token.accessToken(),
            token.tokenType(),
            token.scope(),
            gitHubUser.id(),
            gitHubUser.login(),
            gitHubUser.type()
        ));
    connection.updateOAuthConnection(
        token.accessToken(),
        token.tokenType(),
        token.scope(),
        gitHubUser.id(),
        gitHubUser.login(),
        gitHubUser.type()
    );
    gitHubConnectionRepository.save(connection);
    gitHubPendingConnectionRepository.delete(pending);
    return properties.frontendConnectedRedirectUrl();
  }

  @Transactional(readOnly = true)
  public List<GitHubRepositoryMetadata> listRepositories() {
    GitHubConnectionEntity connection = getRequiredConnection(
        "Connect GitHub before loading repositories."
    );
    return gitHubClient.listAuthenticatedUserRepositories(
        connection.getAccessToken()
    );
  }

  @Transactional(readOnly = true)
  public List<GitHubPullRequestSummary> listPullRequests(String owner, String repo) {
    GitHubConnectionEntity connection = getRequiredConnection(
        "Connect GitHub before loading pull requests."
    );
    return gitHubClient.listPullRequests(
        connection.getAccessToken(),
        new GitHubPullRequestRef(owner, repo, 0)
    );
  }

  @Transactional(readOnly = true)
  public String getConnectedAccessToken() {
    GitHubConnectionEntity connection = getRequiredConnection(
        "Connect GitHub before reviewing pull requests."
    );
    return connection.getAccessToken();
  }

  private GitHubConnectionResponse toResponse(GitHubConnectionEntity connection) {
    return GitHubConnectionResponse.connected(
        connection.getAccountLogin(),
        connection.getAccountType()
    );
  }

  private GitHubConnectionEntity getRequiredConnection(String message) {
    UserEntity user = currentUserService.getCurrentUser();
    return gitHubConnectionRepository.findByUserId(user.getId())
        .orElseThrow(() -> new GitHubConnectionRequiredException(message));
  }

  private GitHubPendingConnectionEntity validatePendingState(UserEntity user, String state) {
    return gitHubPendingConnectionRepository.findByState(state)
        .filter(candidate -> candidate.getUser().getId().equals(user.getId()))
        .filter(candidate -> !candidate.isExpired(Instant.now()))
        .orElseThrow(GitHubSetupStateException::new);
  }

  private String encode(String value) {
    return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
  }
}
