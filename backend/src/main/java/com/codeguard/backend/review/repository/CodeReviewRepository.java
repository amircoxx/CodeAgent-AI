package com.codeguard.backend.review.repository;

import com.codeguard.backend.review.entity.CodeReviewEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CodeReviewRepository extends JpaRepository<CodeReviewEntity, Long> {

  @EntityGraph(attributePaths = {"issues", "project"})
  List<CodeReviewEntity> findAllByOrderByCreatedAtDesc();

  @EntityGraph(attributePaths = {"issues", "project"})
  List<CodeReviewEntity> findAllByUserIdOrderByCreatedAtDesc(Long userId);

  @EntityGraph(attributePaths = {"issues", "project"})
  Optional<CodeReviewEntity> findWithIssuesAndRecommendedTestsById(Long id);

  @EntityGraph(attributePaths = {"issues", "project"})
  Optional<CodeReviewEntity> findByIdAndUserId(Long id, Long userId);
}
