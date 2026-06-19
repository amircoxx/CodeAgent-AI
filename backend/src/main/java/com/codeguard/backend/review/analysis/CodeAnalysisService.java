package com.codeguard.backend.review.analysis;

import com.codeguard.backend.review.dto.ReviewRequest;

public interface CodeAnalysisService {

  CodeAnalysisResult analyzeCode(ReviewRequest request);
}
