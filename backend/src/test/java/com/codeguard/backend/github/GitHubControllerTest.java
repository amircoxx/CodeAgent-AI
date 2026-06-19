package com.codeguard.backend.github;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.codeguard.backend.project.repository.ProjectRepository;
import com.codeguard.backend.review.entity.CodeReviewEntity;
import com.codeguard.backend.review.repository.CodeReviewRepository;
import com.codeguard.backend.user.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:codeguard-github-test;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.jpa.show-sql=false",
    "codeguard.ai.enabled=false",
    "codeguard.github.max-files=20",
    "codeguard.github.max-patch-chars=30000"
})
class GitHubControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private CodeReviewRepository codeReviewRepository;

  @Autowired
  private ProjectRepository projectRepository;

  @Autowired
  private UserRepository userRepository;

  @MockBean
  private GitHubClient gitHubClient;

  @BeforeEach
  void setUp() {
    codeReviewRepository.deleteAll();
    projectRepository.deleteAll();
    userRepository.deleteAll();
  }

  @Test
  void reviewPullRequestFetchesDiffAnalyzesAndSavesReview() throws Exception {
    String token = register("amir@example.com");
    long projectId = createProject(token, "CodeGuard Backend", "Spring Boot backend");
    stubGitHubPullRequest();

    MvcResult result = mockMvc.perform(post("/api/github/pull-request-review")
            .header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(
                new PullRequestReviewRequest(projectId, "https://github.com/owner/repo/pull/123")
            )))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.title").value("GitHub PR Review: owner/repo#123"))
        .andExpect(jsonPath("$.language").value("Multiple"))
        .andExpect(jsonPath("$.projectId").value(projectId))
        .andExpect(jsonPath("$.projectName").value("CodeGuard Backend"))
        .andExpect(jsonPath("$.summary").value(org.hamcrest.Matchers.containsString("Multiple code")))
        .andReturn();

    JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
    CodeReviewEntity savedReview = codeReviewRepository.findAll().getFirst();

    assertThat(savedReview.getId()).isEqualTo(response.get("id").asLong());
    assertThat(savedReview.getUser().getId()).isPositive();
    assertThat(savedReview.getProject().getId()).isEqualTo(projectId);
    assertThat(savedReview.getSubmittedCode())
        .contains("Repository: owner/repo")
        .contains("Pull Request: #123 - Add validation")
        .contains("Author: octocat")
        .contains("File: src/main/java/Example.java")
        .contains("Patch:")
        .doesNotContain("package-lock.json");
  }

  @Test
  void reviewPullRequestReturnsBadRequestForInvalidUrl() throws Exception {
    String token = register("amir@example.com");

    mockMvc.perform(post("/api/github/pull-request-review")
            .header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(
                new PullRequestReviewRequest(null, "https://github.com/owner/repo/issues/123")
            )))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value(
            "GitHub pull request URL must match https://github.com/{owner}/{repo}/pull/{number}"
        ))
        .andExpect(jsonPath("$.path").value("/api/github/pull-request-review"));
  }

  @Test
  void protectedGitHubEndpointRequiresAuth() throws Exception {
    mockMvc.perform(post("/api/github/pull-request-review")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(
                new PullRequestReviewRequest(null, "https://github.com/owner/repo/pull/123")
            )))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.message").value("Your session expired. Please log in again."));
  }

  @Test
  void savedPullRequestReviewAppearsInReviewHistory() throws Exception {
    String token = register("amir@example.com");
    stubGitHubPullRequest();

    mockMvc.perform(post("/api/github/pull-request-review")
            .header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(
                new PullRequestReviewRequest(null, "https://github.com/owner/repo/pull/123")
            )))
        .andExpect(status().isOk());

    mockMvc.perform(get("/api/reviews")
            .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].title").value("GitHub PR Review: owner/repo#123"))
        .andExpect(jsonPath("$[0].language").value("Multiple"));
  }

  private void stubGitHubPullRequest() {
    when(gitHubClient.fetchPullRequest(argThat(pr ->
        "owner".equals(pr.owner()) && "repo".equals(pr.repo()) && pr.number() == 123
    ))).thenReturn(new GitHubPullRequestMetadata("Add validation", "octocat"));
    when(gitHubClient.fetchPullRequestFiles(argThat(pr ->
        "owner".equals(pr.owner()) && "repo".equals(pr.repo()) && pr.number() == 123
    ))).thenReturn(List.of(
        new GitHubPullRequestFile(
            "src/main/java/Example.java",
            "modified",
            "@@ -1,3 +1,6 @@\n public class Example {\n+  void validate() {}\n }"
        ),
        new GitHubPullRequestFile("package-lock.json", "modified", "@@ lockfile noise @@"),
        new GitHubPullRequestFile("assets/logo.png", "modified", null)
    ));
  }

  private long createProject(String token, String name, String description) throws Exception {
    MvcResult result = mockMvc.perform(post("/api/projects")
            .header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(new ProjectRequest(name, description))))
        .andExpect(status().isCreated())
        .andReturn();
    return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
  }

  private String register(String email) throws Exception {
    MvcResult result = mockMvc.perform(post("/api/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(new RegisterRequest("Amir Cox", email, "password123"))))
        .andExpect(status().isCreated())
        .andReturn();
    return objectMapper.readTree(result.getResponse().getContentAsString()).get("token").asText();
  }

  private record PullRequestReviewRequest(Long projectId, String pullRequestUrl) {
  }

  private record ProjectRequest(String name, String description) {
  }

  private record RegisterRequest(String name, String email, String password) {
  }
}
