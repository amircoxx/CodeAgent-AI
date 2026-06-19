package com.codeguard.backend.review;

import com.codeguard.backend.review.dto.ReviewRequest;
import com.codeguard.backend.review.dto.ReviewResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reviews")
public class ReviewController {

  private final ReviewService reviewService;

  public ReviewController(ReviewService reviewService) {
    this.reviewService = reviewService;
  }

  @PostMapping
  @ResponseStatus(HttpStatus.OK)
  public ReviewResponse createReview(@Valid @RequestBody ReviewRequest request) {
    return reviewService.createReview(request);
  }

  @GetMapping
  public List<ReviewResponse> getReviews() {
    return reviewService.getReviews();
  }

  @GetMapping("/{id}")
  public ReviewResponse getReview(@PathVariable Long id) {
    return reviewService.getReview(id);
  }
}
