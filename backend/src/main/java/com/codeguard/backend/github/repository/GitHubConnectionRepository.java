package com.codeguard.backend.github.repository;

import com.codeguard.backend.github.entity.GitHubConnectionEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GitHubConnectionRepository extends JpaRepository<GitHubConnectionEntity, Long> {

  Optional<GitHubConnectionEntity> findByUserId(Long userId);

  Optional<GitHubConnectionEntity> findByUserIdAndInstallationId(Long userId, Long installationId);
}
