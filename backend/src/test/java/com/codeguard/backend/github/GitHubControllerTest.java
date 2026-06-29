package com.codeguard.backend.github;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.codeguard.backend.project.repository.ProjectRepository;
import com.codeguard.backend.github.entity.GitHubConnectionEntity;
import com.codeguard.backend.github.repository.GitHubPendingConnectionRepository;
import com.codeguard.backend.github.repository.GitHubConnectionRepository;
import com.codeguard.backend.review.entity.CodeReviewEntity;
import com.codeguard.backend.review.model.ReviewSource;
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
    "codeguard.github.max-patch-chars=30000",
    "codeguard.github.comments-enabled=true",
    "codeguard.github.token=test-token",
    "codeguard.github.app-slug=codeguard-ai-test",
    "codeguard.github.frontend-connected-redirect-url=http://localhost:3000/?github=connected",
    "codeguard.github.pending-state-ttl-minutes=10"
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
  private GitHubConnectionRepository gitHubConnectionRepository;

  @Autowired
  private GitHubPendingConnectionRepository gitHubPendingConnectionRepository;

  @Autowired
  private UserRepository userRepository;

  @MockBean
  private GitHubClient gitHubClient;

  @MockBean
  private GitHubAppTokenService gitHubAppTokenService;

  @BeforeEach
  void setUp() {
    codeReviewRepository.deleteAll();
    projectRepository.deleteAll();
    gitHubPendingConnectionRepository.deleteAll();
    gitHubConnectionRepository.deleteAll();
    userRepository.deleteAll();
  }

  @Test
  void connectionStatusReturnsDisconnectedWhenUserHasNoGitHubInstallation() throws Exception {
    String token = register("amir@example.com");

    mockMvc.perform(get("/api/github/connection")
            .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.connected").value(false))
        .andExpect(jsonPath("$.accountLogin").doesNotExist())
        .andExpect(jsonPath("$.accountType").doesNotExist());
  }

  @Test
  void connectionStatusReturnsInstallationAccountWhenConnected() throws Exception {
    String token = register("amir@example.com");
    gitHubConnectionRepository.save(new GitHubConnectionEntity(
        userRepository.findActiveByEmail("amir@example.com").orElseThrow(),
        98765L,
        "codeguard-labs",
        "Organization"
    ));

    mockMvc.perform(get("/api/github/connection")
            .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.connected").value(true))
        .andExpect(jsonPath("$.installationId").value(98765))
        .andExpect(jsonPath("$.accountLogin").value("codeguard-labs"))
        .andExpect(jsonPath("$.accountType").value("Organization"));
  }

  @Test
  void connectUrlCreatesPendingStateForCurrentUser() throws Exception {
    String token = register("amir@example.com");
    Long userId = userRepository.findActiveByEmail("amir@example.com").orElseThrow().getId();

    mockMvc.perform(get("/api/github/connect-url")
            .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.connectUrl")
            .value(org.hamcrest.Matchers.startsWith("https://github.com/apps/codeguard-ai-test/installations/new")))
        .andExpect(jsonPath("$.state").isString());

    assertThat(gitHubPendingConnectionRepository.findAll())
        .singleElement()
        .satisfies(pending -> {
          assertThat(pending.getUser().getId()).isEqualTo(userId);
          assertThat(pending.getState()).isNotBlank();
          assertThat(pending.isExpired(java.time.Instant.now())).isFalse();
        });
  }

  @Test
  void setupBindsInstallationToCurrentUserWhenStateIsValid() throws Exception {
    String token = register("amir@example.com");
    Long userId = userRepository.findActiveByEmail("amir@example.com").orElseThrow().getId();
    String state = objectMapper.readTree(mockMvc.perform(get("/api/github/connect-url")
            .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andReturn()
        .getResponse()
        .getContentAsString()).get("state").asText();
    when(gitHubClient.fetchInstallation(eq(98765L)))
        .thenReturn(new GitHubInstallationMetadata(98765L, "codeguard-labs", "Organization"));

    mockMvc.perform(get("/api/github/setup")
            .header("Authorization", "Bearer " + token)
            .param("installation_id", "98765")
            .param("state", state))
        .andExpect(status().is3xxRedirection())
        .andExpect(result -> assertThat(result.getResponse().getRedirectedUrl())
            .isEqualTo("http://localhost:3000/?github=connected"));

    assertThat(gitHubConnectionRepository.findAll())
        .singleElement()
        .satisfies(connection -> {
          assertThat(connection.getUser().getId()).isEqualTo(userId);
          assertThat(connection.getInstallationId()).isEqualTo(98765L);
          assertThat(connection.getAccountLogin()).isEqualTo("codeguard-labs");
          assertThat(connection.getAccountType()).isEqualTo("Organization");
        });
    assertThat(gitHubPendingConnectionRepository.findAll()).isEmpty();
  }

  @Test
  void setupReturnsBadRequestForInvalidState() throws Exception {
    String token = register("amir@example.com");

    mockMvc.perform(get("/api/github/setup")
            .header("Authorization", "Bearer " + token)
            .param("installation_id", "98765")
            .param("state", "not-valid"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("GitHub connection state is invalid or expired."));
  }

  @Test
  void repositoriesRequireGitHubConnection() throws Exception {
    String token = register("amir@example.com");

    mockMvc.perform(get("/api/github/repositories")
            .header("Authorization", "Bearer " + token))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.message").value("Connect GitHub before loading repositories."));
  }

  @Test
  void repositoriesReturnInstallationRepositoriesForConnectedUser() throws Exception {
    String token = register("amir@example.com");
    connectUser("amir@example.com", 98765L);
    when(gitHubAppTokenService.createInstallationAccessToken(98765L))
        .thenReturn("installation-token");
    when(gitHubClient.listInstallationRepositories("installation-token"))
        .thenReturn(List.of(
            new GitHubRepositoryMetadata(101L, "owner", "repo", "owner/repo", false),
            new GitHubRepositoryMetadata(102L, "owner", "private-repo", "owner/private-repo", true)
        ));

    mockMvc.perform(get("/api/github/repositories")
            .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].id").value(101))
        .andExpect(jsonPath("$[0].owner").value("owner"))
        .andExpect(jsonPath("$[0].name").value("repo"))
        .andExpect(jsonPath("$[0].fullName").value("owner/repo"))
        .andExpect(jsonPath("$[0].privateRepository").value(false))
        .andExpect(jsonPath("$[1].id").value(102))
        .andExpect(jsonPath("$[1].privateRepository").value(true));
  }

  @Test
  void pullRequestsReturnOpenPullRequestsForSelectedRepository() throws Exception {
    String token = register("amir@example.com");
    connectUser("amir@example.com", 98765L);
    when(gitHubAppTokenService.createInstallationAccessToken(98765L))
        .thenReturn("installation-token");
    when(gitHubClient.listPullRequests(
        eq("installation-token"),
        argThat(ref -> "owner".equals(ref.owner()) && "repo".equals(ref.repo()) && ref.number() == 0)
    )).thenReturn(List.of(
        new GitHubPullRequestSummary(
            123,
            "Add validation",
            "octocat",
            "https://github.com/owner/repo/pull/123"
        )
    ));

    mockMvc.perform(get("/api/github/repositories/{owner}/{repo}/pull-requests", "owner", "repo")
            .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].number").value(123))
        .andExpect(jsonPath("$[0].title").value("Add validation"))
        .andExpect(jsonPath("$[0].author").value("octocat"))
        .andExpect(jsonPath("$[0].url").value("https://github.com/owner/repo/pull/123"));
  }

  @Test
  void selectedPullRequestReviewUsesConnectedInstallationTokenAndSavesReview() throws Exception {
    String token = register("amir@example.com");
    connectUser("amir@example.com", 98765L);
    when(gitHubAppTokenService.createInstallationAccessToken(98765L))
        .thenReturn("installation-token");
    stubGitHubPullRequest("installation-token");

    mockMvc.perform(post("/api/github/pull-request-review")
            .header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(
                new SelectedPullRequestReviewRequest(null, "owner", "repo", 123, true)
            )))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.source").value("GITHUB_PR"))
        .andExpect(jsonPath("$.title").value("GitHub PR Review: owner/repo#123"))
        .andExpect(jsonPath("$.githubOwner").value("owner"))
        .andExpect(jsonPath("$.githubRepo").value("repo"))
        .andExpect(jsonPath("$.githubPullRequestNumber").value(123))
        .andExpect(jsonPath("$.githubPullRequestUrl").value("https://github.com/owner/repo/pull/123"))
        .andExpect(jsonPath("$.githubPullRequestTitle").value("Add validation"))
        .andExpect(jsonPath("$.githubCommentPosted").value(false))
        .andExpect(jsonPath("$.githubCommentUrl").doesNotExist())
        .andExpect(jsonPath("$.githubCommentError").doesNotExist());

    CodeReviewEntity savedReview = codeReviewRepository.findAll().getFirst();
    assertThat(savedReview.getSource()).isEqualTo(ReviewSource.GITHUB_PR);
    assertThat(savedReview.getGithubOwner()).isEqualTo("owner");
    assertThat(savedReview.getGithubRepo()).isEqualTo("repo");
    assertThat(savedReview.getGithubPullRequestNumber()).isEqualTo(123);
    verify(gitHubClient).fetchPullRequest(
        eq("installation-token"),
        argThat(pr -> "owner".equals(pr.owner()) && "repo".equals(pr.repo()) && pr.number() == 123)
    );
    verify(gitHubClient).fetchPullRequestFiles(
        eq("installation-token"),
        argThat(pr -> "owner".equals(pr.owner()) && "repo".equals(pr.repo()) && pr.number() == 123)
    );
    verify(gitHubClient, never()).createPullRequestComment(argThat(pr -> true), anyString());
  }

  @Test
  void selectedPullRequestReviewRequiresGitHubConnection() throws Exception {
    String token = register("amir@example.com");

    mockMvc.perform(post("/api/github/pull-request-review")
            .header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(
                new SelectedPullRequestReviewRequest(null, "owner", "repo", 123, false)
            )))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.message").value("Connect GitHub before reviewing pull requests."));
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
        .andExpect(jsonPath("$.source").value("GITHUB_PR"))
        .andExpect(jsonPath("$.githubOwner").value("owner"))
        .andExpect(jsonPath("$.githubRepo").value("repo"))
        .andExpect(jsonPath("$.githubPullRequestNumber").value(123))
        .andExpect(jsonPath("$.githubPullRequestUrl").value("https://github.com/owner/repo/pull/123"))
        .andExpect(jsonPath("$.githubPullRequestTitle").value("Add validation"))
        .andExpect(jsonPath("$.githubCommentPosted").value(false))
        .andExpect(jsonPath("$.githubCommentUrl").doesNotExist())
        .andExpect(jsonPath("$.githubCommentError").doesNotExist())
        .andExpect(jsonPath("$.projectId").value(projectId))
        .andExpect(jsonPath("$.projectName").value("CodeGuard Backend"))
        .andExpect(jsonPath("$.summary").value(org.hamcrest.Matchers.containsString("Multiple code")))
        .andReturn();

    JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
    CodeReviewEntity savedReview = codeReviewRepository.findAll().getFirst();

    assertThat(savedReview.getId()).isEqualTo(response.get("id").asLong());
    assertThat(savedReview.getUser().getId()).isPositive();
    assertThat(savedReview.getProject().getId()).isEqualTo(projectId);
    assertThat(savedReview.getSource()).isEqualTo(ReviewSource.GITHUB_PR);
    assertThat(savedReview.getGithubOwner()).isEqualTo("owner");
    assertThat(savedReview.getGithubRepo()).isEqualTo("repo");
    assertThat(savedReview.getGithubPullRequestNumber()).isEqualTo(123);
    assertThat(savedReview.getGithubPullRequestUrl()).isEqualTo("https://github.com/owner/repo/pull/123");
    assertThat(savedReview.getGithubPullRequestTitle()).isEqualTo("Add validation");
    assertThat(savedReview.getSubmittedCode())
        .contains("Repository: owner/repo")
        .contains("Pull Request: #123 - Add validation")
        .contains("Author: octocat")
        .contains("File: src/main/java/Example.java")
        .contains("Patch:")
        .doesNotContain("package-lock.json");
    verify(gitHubClient, never()).createPullRequestComment(argThat(pr -> true), anyString());
  }

  @Test
  void reviewPullRequestCanPostSummaryCommentWhenRequested() throws Exception {
    String token = register("amir@example.com");
    stubGitHubPullRequest();
    when(gitHubClient.createPullRequestComment(argThat(pr ->
        "owner".equals(pr.owner()) && "repo".equals(pr.repo()) && pr.number() == 123
    ), argThat(body ->
        body.contains("<!-- codeguard-ai-review -->")
            && body.contains("## CodeGuard AI Review")
            && body.contains("### Top Issues")
            && body.contains("Generated by CodeGuard AI.")
    ))).thenReturn("https://github.com/owner/repo/issues/123#issuecomment-1");

    MvcResult result = mockMvc.perform(post("/api/github/pull-request-review")
            .header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(
                new PullRequestReviewRequest(
                    null,
                    "https://github.com/owner/repo/pull/123",
                    true
                )
            )))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.source").value("GITHUB_PR"))
        .andExpect(jsonPath("$.githubCommentPosted").value(true))
        .andExpect(jsonPath("$.githubCommentUrl")
            .value("https://github.com/owner/repo/issues/123#issuecomment-1"))
        .andExpect(jsonPath("$.githubCommentError").doesNotExist())
        .andReturn();

    long reviewId = objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();

    mockMvc.perform(get("/api/reviews/{id}", reviewId)
            .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.githubCommentPosted").value(true))
        .andExpect(jsonPath("$.githubCommentUrl")
            .value("https://github.com/owner/repo/issues/123#issuecomment-1"))
        .andExpect(jsonPath("$.githubCommentError").doesNotExist());
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
        .andExpect(jsonPath("$[0].language").value("Multiple"))
        .andExpect(jsonPath("$[0].source").value("GITHUB_PR"))
        .andExpect(jsonPath("$[0].githubOwner").value("owner"))
        .andExpect(jsonPath("$[0].githubRepo").value("repo"))
        .andExpect(jsonPath("$[0].githubPullRequestNumber").value(123))
        .andExpect(jsonPath("$[0].githubPullRequestTitle").value("Add validation"));
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

  private void stubGitHubPullRequest(String installationToken) {
    when(gitHubClient.fetchPullRequest(eq(installationToken), argThat(pr ->
        "owner".equals(pr.owner()) && "repo".equals(pr.repo()) && pr.number() == 123
    ))).thenReturn(new GitHubPullRequestMetadata("Add validation", "octocat"));
    when(gitHubClient.fetchPullRequestFiles(eq(installationToken), argThat(pr ->
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

  private void connectUser(String email, Long installationId) {
    gitHubConnectionRepository.save(new GitHubConnectionEntity(
        userRepository.findActiveByEmail(email).orElseThrow(),
        installationId,
        "codeguard-labs",
        "Organization"
    ));
  }

  private String register(String email) throws Exception {
    MvcResult result = mockMvc.perform(post("/api/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(new RegisterRequest("Amir Cox", email, "password123"))))
        .andExpect(status().isCreated())
        .andReturn();
    return objectMapper.readTree(result.getResponse().getContentAsString()).get("token").asText();
  }

  private record PullRequestReviewRequest(Long projectId, String pullRequestUrl, Boolean postComment) {

    private PullRequestReviewRequest(Long projectId, String pullRequestUrl) {
      this(projectId, pullRequestUrl, null);
    }
  }

  private record SelectedPullRequestReviewRequest(
      Long projectId,
      String owner,
      String repo,
      Integer pullRequestNumber,
      Boolean postComment
  ) {
  }

  private record ProjectRequest(String name, String description) {
  }

  private record RegisterRequest(String name, String email, String password) {
  }
}
