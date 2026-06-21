package com.codeguard.backend.review;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.codeguard.backend.project.repository.ProjectRepository;
import com.codeguard.backend.review.entity.CodeReviewEntity;
import com.codeguard.backend.review.model.ReviewSource;
import com.codeguard.backend.review.repository.CodeReviewRepository;
import com.codeguard.backend.user.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:codeguard-test;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.jpa.show-sql=false"
})
class ReviewControllerTest {

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

  @Autowired
  private JdbcTemplate jdbcTemplate;

  @BeforeEach
  void setUp() {
    codeReviewRepository.deleteAll();
    projectRepository.deleteAll();
    userRepository.deleteAll();
  }

  @Test
  void createReviewSavesReview() throws Exception {
    String token = register("amir@example.com");
    createReview(token, "Login Controller Review", "Java", "public class LoginController {}");

    assertThat(codeReviewRepository.findAll()).hasSize(1);
    CodeReviewEntity savedReview = codeReviewRepository.findAll().getFirst();
    assertThat(savedReview.getTitle()).isEqualTo("Login Controller Review");
    assertThat(savedReview.getLanguage()).isEqualTo("Java");
    assertThat(savedReview.getSubmittedCode()).contains("LoginController");
    assertThat(savedReview.getSource()).isEqualTo(ReviewSource.MANUAL);
    assertThat(savedReview.getGithubOwner()).isNull();
    assertThat(savedReview.getProject()).isNull();
    assertThat(savedReview.getUser().getId()).isPositive();
  }

  @Test
  void createReviewReturnsStructuredPersistedResponse() throws Exception {
    String token = register("amir@example.com");
    MvcResult result = createReview(
        token,
        "Login Controller Review",
        "Java",
        "public class LoginController {}"
    );

    JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());

    assertThat(response.get("id").asLong()).isPositive();
    assertThat(response.get("title").asText()).isEqualTo("Login Controller Review");
    assertThat(response.get("summary").asText()).contains("Java code");
    assertThat(response.get("riskScore").asInt()).isEqualTo(78);
    assertThat(response.get("source").asText()).isEqualTo("MANUAL");
    assertThat(response.path("githubOwner").isMissingNode() || response.path("githubOwner").isNull()).isTrue();
    assertThat(response.path("githubRepo").isMissingNode() || response.path("githubRepo").isNull()).isTrue();
    assertThat(response.path("githubPullRequestNumber").isMissingNode()
        || response.path("githubPullRequestNumber").isNull()).isTrue();
    assertThat(response.path("githubPullRequestUrl").isMissingNode()
        || response.path("githubPullRequestUrl").isNull()).isTrue();
    assertThat(response.path("githubPullRequestTitle").isMissingNode()
        || response.path("githubPullRequestTitle").isNull()).isTrue();
    assertThat(response.get("createdAt").asText()).isNotBlank();
    assertThat(response.get("issues")).isNotEmpty();
    assertThat(response.get("recommendedTests")).isNotEmpty();
  }

  @Test
  void createReviewWithProjectIdSavesReviewUnderProject() throws Exception {
    String token = register("amir@example.com");
    long projectId = createProject(token, "CodeGuard Backend", "Spring Boot backend code review project");

    MvcResult result = createReview(
        token,
        projectId,
        "Login Controller Review",
        "Java",
        "public class LoginController {}"
    );

    JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
    CodeReviewEntity savedReview = codeReviewRepository.findAll().getFirst();

    assertThat(savedReview.getProject().getId()).isEqualTo(projectId);
    assertThat(response.get("projectId").asLong()).isEqualTo(projectId);
    assertThat(response.get("projectName").asText()).isEqualTo("CodeGuard Backend");
  }

  @Test
  void createReviewWithInvalidProjectIdReturnsNotFound() throws Exception {
    String token = register("amir@example.com");
    String requestBody = objectMapper.writeValueAsString(
        new CreateReviewRequest(999L, "Missing Project Review", "Java", "class Missing {}")
    );

    mockMvc.perform(post("/api/reviews")
            .header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestBody))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.message").value("Project not found: 999"))
        .andExpect(jsonPath("$.status").value(404))
        .andExpect(jsonPath("$.path").value("/api/reviews"))
        .andExpect(jsonPath("$.timestamp").isString());
  }

  @Test
  void getReviewsReturnsSavedReviewsNewestFirst() throws Exception {
    String token = register("amir@example.com");
    createReview(token, "Older Review", "Java", "class Older {}");
    createReview(token, "Newer Review", "JavaScript", "function newer() {}");

    mockMvc.perform(get("/api/reviews")
            .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].title").value("Newer Review"))
        .andExpect(jsonPath("$[1].title").value("Older Review"))
        .andExpect(jsonPath("$[0].issues").isArray())
        .andExpect(jsonPath("$[0].recommendedTests").isArray());
  }

  @Test
  void getReviewsNormalizesLegacyIssueValues() throws Exception {
    String token = register("amir@example.com");
    createReview(token, "Legacy Bug Review", "Java", "class LegacyBug {}");
    createReview(token, "Legacy Style Review", "Java", "class LegacyStyle {}");
    createReview(token, "Unknown Category Review", "Java", "class UnknownCategory {}");

    jdbcTemplate.update("""
        update review_issues
        set category = 'BUG'
        where id = (
          select min(ri.id)
          from review_issues ri
          join code_reviews cr on cr.id = ri.code_review_id
          where cr.title = 'Legacy Bug Review'
        )
        """);
    jdbcTemplate.update("""
        update review_issues
        set category = 'STYLE'
        where id = (
          select min(ri.id)
          from review_issues ri
          join code_reviews cr on cr.id = ri.code_review_id
          where cr.title = 'Legacy Style Review'
        )
        """);
    jdbcTemplate.update("""
        update review_issues
        set category = 'CORRECTNESS',
            severity = 'BLOCKER'
        where id = (
          select min(ri.id)
          from review_issues ri
          join code_reviews cr on cr.id = ri.code_review_id
          where cr.title = 'Unknown Category Review'
        )
        """);

    mockMvc.perform(get("/api/reviews")
            .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[?(@.title == 'Legacy Bug Review')].issues[0].category")
            .value(org.hamcrest.Matchers.contains("BUG_RISK")))
        .andExpect(jsonPath("$[?(@.title == 'Legacy Style Review')].issues[0].category")
            .value(org.hamcrest.Matchers.contains("READABILITY")))
        .andExpect(jsonPath("$[?(@.title == 'Unknown Category Review')].issues[0].category")
            .value(org.hamcrest.Matchers.contains("MAINTAINABILITY")))
        .andExpect(jsonPath("$[?(@.title == 'Unknown Category Review')].issues[0].severity")
            .value(org.hamcrest.Matchers.contains("MEDIUM")));
  }

  @Test
  void getReviewReturnsCorrectReview() throws Exception {
    String token = register("amir@example.com");
    MvcResult created = createReview(
        token,
        "Payment Service Review",
        "Java",
        "public class PaymentService {}"
    );
    long id = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asLong();

    mockMvc.perform(get("/api/reviews/{id}", id)
            .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(id))
        .andExpect(jsonPath("$.projectId").doesNotExist())
        .andExpect(jsonPath("$.projectName").doesNotExist())
        .andExpect(jsonPath("$.title").value("Payment Service Review"))
        .andExpect(jsonPath("$.language").value("Java"))
        .andExpect(jsonPath("$.source").value("MANUAL"))
        .andExpect(jsonPath("$.githubOwner").doesNotExist())
        .andExpect(jsonPath("$.githubRepo").doesNotExist())
        .andExpect(jsonPath("$.githubPullRequestNumber").doesNotExist())
        .andExpect(jsonPath("$.githubPullRequestUrl").doesNotExist())
        .andExpect(jsonPath("$.githubPullRequestTitle").doesNotExist())
        .andExpect(jsonPath("$.issues[0].title").value("Missing input validation"))
        .andExpect(jsonPath("$.recommendedTests[0]").value("Test Payment Service Review with invalid input"));
  }

  @Test
  void getReviewReturnsNotFoundForMissingReview() throws Exception {
    String token = register("amir@example.com");

    mockMvc.perform(get("/api/reviews/{id}", 999L)
            .header("Authorization", "Bearer " + token))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.message").value("Review not found: 999"))
        .andExpect(jsonPath("$.status").value(404))
        .andExpect(jsonPath("$.path").value("/api/reviews/999"))
        .andExpect(jsonPath("$.timestamp").isString());
  }

  @Test
  void protectedReviewEndpointsRequireAuth() throws Exception {
    mockMvc.perform(get("/api/reviews"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.message").value("Your session expired. Please log in again."))
        .andExpect(jsonPath("$.status").value(401))
        .andExpect(jsonPath("$.path").value("/api/reviews"))
        .andExpect(jsonPath("$.timestamp").isString());
  }

  @Test
  void createReviewValidationReturnsCleanErrorShape() throws Exception {
    String token = register("amir@example.com");
    String requestBody = objectMapper.writeValueAsString(
        new CreateReviewRequest(null, "", "", "")
    );

    mockMvc.perform(post("/api/reviews")
            .header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestBody))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Title is required")))
        .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Language is required")))
        .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Code is required")))
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.path").value("/api/reviews"))
        .andExpect(jsonPath("$.timestamp").isString());
  }

  @Test
  void getReviewsReturnsOnlyCurrentUsersReviews() throws Exception {
    String amirToken = register("amir@example.com");
    String taylorToken = register("taylor@example.com");
    createReview(amirToken, "Amir Review", "Java", "class Amir {}");
    createReview(taylorToken, "Taylor Review", "Java", "class Taylor {}");

    mockMvc.perform(get("/api/reviews")
            .header("Authorization", "Bearer " + amirToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].title").value("Amir Review"));
  }

  @Test
  void getReviewReturnsNotFoundForAnotherUsersReview() throws Exception {
    String amirToken = register("amir@example.com");
    String taylorToken = register("taylor@example.com");
    MvcResult created = createReview(taylorToken, "Taylor Review", "Java", "class Taylor {}");
    long id = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asLong();

    mockMvc.perform(get("/api/reviews/{id}", id)
            .header("Authorization", "Bearer " + amirToken))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.message").value("Review not found: " + id))
        .andExpect(jsonPath("$.status").value(404));
  }

  @Test
  void createReviewCannotAttachAnotherUsersProject() throws Exception {
    String amirToken = register("amir@example.com");
    String taylorToken = register("taylor@example.com");
    long taylorProjectId = createProject(taylorToken, "Taylor Project", "Owned by Taylor");

    String requestBody = objectMapper.writeValueAsString(
        new CreateReviewRequest(taylorProjectId, "Cross User Review", "Java", "class CrossUser {}")
    );

    mockMvc.perform(post("/api/reviews")
            .header("Authorization", "Bearer " + amirToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestBody))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.message").value("Project not found: " + taylorProjectId))
        .andExpect(jsonPath("$.status").value(404));
  }

  private MvcResult createReview(String token, String title, String language, String code) throws Exception {
    return createReview(token, null, title, language, code);
  }

  private MvcResult createReview(String token, Long projectId, String title, String language, String code) throws Exception {
    String requestBody = objectMapper.writeValueAsString(new CreateReviewRequest(projectId, title, language, code));

    return mockMvc.perform(post("/api/reviews")
            .header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestBody))
        .andExpect(status().isOk())
        .andReturn();
  }

  private long createProject(String token, String name, String description) throws Exception {
    MvcResult result = mockMvc.perform(post("/api/projects")
            .header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(new CreateProjectRequest(name, description))))
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

  private record CreateReviewRequest(Long projectId, String title, String language, String code) {
  }

  private record CreateProjectRequest(String name, String description) {
  }

  private record RegisterRequest(String name, String email, String password) {
  }
}
