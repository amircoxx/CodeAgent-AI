package com.codeguard.backend.project.repository;

import com.codeguard.backend.project.entity.ProjectEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectRepository extends JpaRepository<ProjectEntity, Long> {

  List<ProjectEntity> findAllByOrderByCreatedAtDesc();

  List<ProjectEntity> findAllByUserIdOrderByCreatedAtDesc(Long userId);

  Optional<ProjectEntity> findByIdAndUserId(Long id, Long userId);
}
