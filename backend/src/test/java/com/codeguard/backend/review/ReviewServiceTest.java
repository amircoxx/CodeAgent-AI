package com.codeguard.backend.review;

import static org.assertj.core.api.Assertions.assertThat;

import com.codeguard.backend.project.repository.ProjectRepository;
import com.codeguard.backend.review.dto.ReviewRequest;
import com.codeguard.backend.review.dto.ReviewResponse;
import com.codeguard.backend.review.entity.CodeReviewEntity;
import com.codeguard.backend.review.model.ReviewSource;
import com.codeguard.backend.review.repository.CodeReviewRepository;
import com.codeguard.backend.user.entity.UserEntity;
import com.codeguard.backend.user.repository.UserRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:codeguard-service-test;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.jpa.show-sql=false",
    "codeguard.ai.enabled=false"
})
class ReviewServiceTest {

  @Autowired
  private ReviewService reviewService;

  @Autowired
  private CodeReviewRepository codeReviewRepository;

  @Autowired
  private ProjectRepository projectRepository;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private PasswordEncoder passwordEncoder;

  @BeforeEach
  void setUp() {
    SecurityContextHolder.clearContext();
    codeReviewRepository.deleteAll();
    projectRepository.deleteAll();
    userRepository.deleteAll();
  }

  @Test
  void createReviewSavesReviewUsingMockAnalyzerWhenAiIsDisabled() {
    authenticate("amir@example.com");

    ReviewResponse response = reviewService.createReview(
        new ReviewRequest(null, "Service Layer Review", "Java", "class ServiceLayer {}")
    );

    assertThat(response.id()).isPositive();
    assertThat(response.summary()).contains("Java code");
    assertThat(response.riskScore()).isEqualTo(78);
    assertThat(response.source()).isEqualTo(ReviewSource.MANUAL);
    assertThat(response.githubOwner()).isNull();
    assertThat(response.githubRepo()).isNull();
    assertThat(response.githubPullRequestNumber()).isNull();
    assertThat(response.githubPullRequestUrl()).isNull();
    assertThat(response.githubPullRequestTitle()).isNull();
    assertThat(response.issues()).isNotEmpty();
    assertThat(response.recommendedTests()).isNotEmpty();

    assertThat(codeReviewRepository.findAll()).hasSize(1);
    CodeReviewEntity savedReview = codeReviewRepository.findWithIssuesAndRecommendedTestsById(response.id())
        .orElseThrow();
    assertThat(savedReview.getTitle()).isEqualTo("Service Layer Review");
    assertThat(savedReview.getSource()).isEqualTo(ReviewSource.MANUAL);
    assertThat(savedReview.getGithubOwner()).isNull();
    assertThat(savedReview.getIssues()).hasSize(3);
    assertThat(savedReview.getUser().getId()).isPositive();
  }

  private void authenticate(String email) {
    UserEntity user = userRepository.save(new UserEntity(
        "Amir Cox",
        email,
        passwordEncoder.encode("password123")
    ));
    SecurityContextHolder.getContext().setAuthentication(
        new UsernamePasswordAuthenticationToken(user.getEmail(), null, List.of())
    );
  }
}
