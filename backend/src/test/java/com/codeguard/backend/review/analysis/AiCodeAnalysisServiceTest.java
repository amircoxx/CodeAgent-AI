package com.codeguard.backend.review.analysis;

import static org.assertj.core.api.Assertions.assertThat;

import com.codeguard.backend.config.CodeGuardAiProperties;
import com.codeguard.backend.review.dto.ReviewRequest;
import com.codeguard.backend.review.model.IssueCategory;
import com.codeguard.backend.review.model.Severity;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import org.junit.jupiter.api.Test;

class AiCodeAnalysisServiceTest {

  private final MockCodeAnalysisService mockCodeAnalysisService = new MockCodeAnalysisService();
  private final ObjectMapper objectMapper = new ObjectMapper();

  @Test
  void aiDisabledUsesMockAnalyzer() {
    AiCodeAnalysisService service = newService(
        new CodeGuardAiProperties(false, "", "test-model", "http://example.test", 5),
        prompt -> {
          throw new AssertionError("AI client should not be called when AI is disabled");
        }
    );

    CodeAnalysisResult result = service.analyzeCode(reviewRequest());

    assertMockResult(result);
  }

  @Test
  void aiFailureFallsBackToMockAnalyzer() {
    AiCodeAnalysisService service = newService(
        enabledProperties(),
        prompt -> {
          throw new IOException("provider unavailable");
        }
    );

    CodeAnalysisResult result = service.analyzeCode(reviewRequest());

    assertMockResult(result);
  }

  @Test
  void invalidAiJsonFallsBackToMockAnalyzer() {
    AiCodeAnalysisService service = newService(
        enabledProperties(),
        prompt -> "{not-json"
    );

    CodeAnalysisResult result = service.analyzeCode(reviewRequest());

    assertMockResult(result);
  }

  @Test
  void validAiJsonReturnsAiAnalysis() {
    AiCodeAnalysisService service = newService(
        enabledProperties(),
        prompt -> """
            {
              "summary": "The code is small but needs validation.",
              "riskScore": 42,
              "issues": [
                {
                  "title": "Add validation",
                  "severity": "MEDIUM",
                  "category": "SECURITY",
                  "explanation": "Input is accepted without a visible guard.",
                  "suggestion": "Validate input before use.",
                  "lineNumber": 3
                }
              ],
              "recommendedTests": ["Test invalid input"]
            }
            """
    );

    CodeAnalysisResult result = service.analyzeCode(reviewRequest());

    assertThat(result.summary()).isEqualTo("The code is small but needs validation.");
    assertThat(result.riskScore()).isEqualTo(42);
    assertThat(result.issues()).hasSize(1);
    assertThat(result.issues().getFirst().severity()).isEqualTo(Severity.MEDIUM);
    assertThat(result.issues().getFirst().category()).isEqualTo(IssueCategory.SECURITY);
    assertThat(result.issues().getFirst().lineNumber()).isEqualTo(3);
    assertThat(result.recommendedTests()).containsExactly("Test invalid input");
  }

  private AiCodeAnalysisService newService(
      CodeGuardAiProperties properties,
      AiChatClient aiChatClient
  ) {
    return new AiCodeAnalysisService(
        properties,
        mockCodeAnalysisService,
        aiChatClient,
        objectMapper
    );
  }

  private CodeGuardAiProperties enabledProperties() {
    return new CodeGuardAiProperties(true, "test-key", "test-model", "http://example.test", 5);
  }

  private ReviewRequest reviewRequest() {
    return new ReviewRequest(null, "Login Review", "Java", "class Login {}");
  }

  private void assertMockResult(CodeAnalysisResult result) {
    assertThat(result.summary()).contains("Java code");
    assertThat(result.riskScore()).isEqualTo(78);
    assertThat(result.issues()).hasSize(3);
    assertThat(result.recommendedTests()).hasSize(3);
  }
}
