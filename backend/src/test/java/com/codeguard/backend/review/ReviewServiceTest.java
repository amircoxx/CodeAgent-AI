package com.codeguard.backend.review;

import static org.assertj.core.api.Assertions.assertThat;

import com.codeguard.backend.review.dto.ReviewRequest;
import com.codeguard.backend.review.dto.ReviewResponse;
import com.codeguard.backend.review.model.RiskLevel;
import org.junit.jupiter.api.Test;

class ReviewServiceTest {

  private final ReviewService reviewService = new ReviewService();

  @Test
  void reviewCodeReturnsStructuredMockResponse() {
    ReviewResponse response = reviewService.reviewCode(
        new ReviewRequest("JavaScript", "function test() { console.log('hi') }")
    );

    assertThat(response.language()).isEqualTo("JavaScript");
    assertThat(response.riskLevel()).isEqualTo(RiskLevel.MEDIUM);
    assertThat(response.issues()).isNotEmpty();
    assertThat(response.improvedCode()).contains("function test");
  }
}
