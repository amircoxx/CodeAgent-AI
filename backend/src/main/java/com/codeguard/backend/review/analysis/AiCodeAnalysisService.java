package com.codeguard.backend.review.analysis;

import com.codeguard.backend.shared.config.CodeGuardAiProperties;
import com.codeguard.backend.review.dto.ReviewIssue;
import com.codeguard.backend.review.dto.ReviewRequest;
import com.codeguard.backend.review.model.IssueCategory;
import com.codeguard.backend.review.model.Severity;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

@Primary
@Service
public class AiCodeAnalysisService implements CodeAnalysisService {

  private final CodeGuardAiProperties properties;
  private final MockCodeAnalysisService mockCodeAnalysisService;
  private final AiChatClient aiChatClient;
  private final ObjectMapper objectMapper;

  public AiCodeAnalysisService(
      CodeGuardAiProperties properties,
      MockCodeAnalysisService mockCodeAnalysisService,
      AiChatClient aiChatClient,
      ObjectMapper objectMapper
  ) {
    this.properties = properties;
    this.mockCodeAnalysisService = mockCodeAnalysisService;
    this.aiChatClient = aiChatClient;
    this.objectMapper = objectMapper;
  }

  @Override
  public CodeAnalysisResult analyzeCode(ReviewRequest request) {
    if (!properties.enabled() || !properties.hasApiKey()) {
      return mockCodeAnalysisService.analyzeCode(request);
    }

    try {
      String json = aiChatClient.completeJson(buildPrompt(request));
      return parseAndValidate(json);
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
      return mockCodeAnalysisService.analyzeCode(request);
    } catch (Exception ex) {
      return mockCodeAnalysisService.analyzeCode(request);
    }
  }

  private String buildPrompt(ReviewRequest request) {
    return """
        Review the submitted code as a senior software engineer and security reviewer.

        Return strict JSON only. Do not include Markdown, comments, or explanatory prose outside JSON.

        Required JSON shape:
        {
          "summary": "Short summary of the submitted code.",
          "riskScore": 72,
          "issues": [
            {
              "title": "Missing input validation",
              "severity": "HIGH",
              "category": "SECURITY",
              "explanation": "The code accepts user input without validating it.",
              "suggestion": "Add DTO validation before the service layer.",
              "lineNumber": 12
            }
          ],
          "recommendedTests": [
            "Test invalid input",
            "Test unauthorized access",
            "Test expected successful path"
          ]
        }

        Valid severity values: LOW, MEDIUM, HIGH, CRITICAL.
        Valid category values: SECURITY, PERFORMANCE, MAINTAINABILITY, TESTING, READABILITY, BUG_RISK, ARCHITECTURE.
        riskScore must be an integer from 0 to 100.
        issues and recommendedTests must be arrays. lineNumber may be null.

        Review title: %s
        Language: %s
        Code:
        ```%s
        %s
        ```
        """.formatted(
        request.title().trim(),
        request.language().trim(),
        request.language().trim(),
        request.code()
    );
  }

  private CodeAnalysisResult parseAndValidate(String json) throws Exception {
    JsonNode root = objectMapper.readTree(json);

    String summary = requiredText(root, "summary");
    int riskScore = requiredRiskScore(root.path("riskScore"));
    List<ReviewIssue> issues = parseIssues(root.path("issues"));
    List<String> recommendedTests = parseRecommendedTests(root.path("recommendedTests"));

    return new CodeAnalysisResult(summary, riskScore, issues, recommendedTests);
  }

  private List<ReviewIssue> parseIssues(JsonNode issuesNode) {
    if (!issuesNode.isArray()) {
      throw new IllegalArgumentException("issues must be an array");
    }

    List<ReviewIssue> issues = new ArrayList<>();
    for (JsonNode issueNode : issuesNode) {
      issues.add(new ReviewIssue(
          requiredText(issueNode, "title"),
          Severity.valueOf(requiredText(issueNode, "severity")),
          IssueCategory.valueOf(requiredText(issueNode, "category")),
          requiredText(issueNode, "explanation"),
          requiredText(issueNode, "suggestion"),
          optionalLineNumber(issueNode.path("lineNumber"))
      ));
    }

    return issues;
  }

  private List<String> parseRecommendedTests(JsonNode testsNode) {
    if (!testsNode.isArray()) {
      throw new IllegalArgumentException("recommendedTests must be an array");
    }

    List<String> recommendedTests = new ArrayList<>();
    for (JsonNode testNode : testsNode) {
      if (!testNode.isTextual() || testNode.asText().isBlank()) {
        throw new IllegalArgumentException("recommendedTests must contain nonblank strings");
      }
      recommendedTests.add(testNode.asText().trim());
    }

    return recommendedTests;
  }

  private String requiredText(JsonNode node, String fieldName) {
    JsonNode value = node.path(fieldName);
    if (!value.isTextual() || value.asText().isBlank()) {
      throw new IllegalArgumentException(fieldName + " is required");
    }

    return value.asText().trim();
  }

  private int requiredRiskScore(JsonNode node) {
    if (!node.canConvertToInt()) {
      throw new IllegalArgumentException("riskScore must be an integer");
    }

    int riskScore = node.asInt();
    if (riskScore < 0 || riskScore > 100) {
      throw new IllegalArgumentException("riskScore must be between 0 and 100");
    }

    return riskScore;
  }

  private Integer optionalLineNumber(JsonNode node) {
    if (node.isMissingNode() || node.isNull()) {
      return null;
    }

    if (!node.canConvertToInt()) {
      throw new IllegalArgumentException("lineNumber must be an integer or null");
    }

    return node.asInt();
  }
}
