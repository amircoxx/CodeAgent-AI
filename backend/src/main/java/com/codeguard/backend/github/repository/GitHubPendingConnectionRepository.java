package com.codeguard.backend.github.repository;

import com.codeguard.backend.github.entity.GitHubPendingConnectionEntity;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GitHubPendingConnectionRepository extends JpaRepository<GitHubPendingConnectionEntity, Long> {

  Optional<GitHubPendingConnectionEntity> findByState(String state);

  void deleteAllByUserId(Long userId);

  void deleteAllByExpiresAtBefore(Instant now);
}
