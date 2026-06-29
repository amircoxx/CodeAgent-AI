package com.codeguard.backend.github;

import com.codeguard.backend.auth.CurrentUserService;
import com.codeguard.backend.github.dto.GitHubConnectionResponse;
import com.codeguard.backend.github.entity.GitHubConnectionEntity;
import com.codeguard.backend.github.repository.GitHubConnectionRepository;
import com.codeguard.backend.user.entity.UserEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GitHubConnectionService {

  private final CurrentUserService currentUserService;
  private final GitHubConnectionRepository gitHubConnectionRepository;

  public GitHubConnectionService(
      CurrentUserService currentUserService,
      GitHubConnectionRepository gitHubConnectionRepository
  ) {
    this.currentUserService = currentUserService;
    this.gitHubConnectionRepository = gitHubConnectionRepository;
  }

  @Transactional(readOnly = true)
  public GitHubConnectionResponse getConnection() {
    UserEntity user = currentUserService.getCurrentUser();
    return gitHubConnectionRepository.findByUserId(user.getId())
        .map(this::toResponse)
        .orElseGet(GitHubConnectionResponse::disconnected);
  }

  private GitHubConnectionResponse toResponse(GitHubConnectionEntity connection) {
    return GitHubConnectionResponse.connected(
        connection.getInstallationId(),
        connection.getAccountLogin(),
        connection.getAccountType()
    );
  }
}
