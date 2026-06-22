package com.codeguard.backend.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.codeguard.backend.account.repository.PasswordChangeVerificationRepository;
import com.codeguard.backend.project.repository.ProjectRepository;
import com.codeguard.backend.review.repository.CodeReviewRepository;
import com.codeguard.backend.user.entity.UserEntity;
import com.codeguard.backend.user.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:codeguard-auth-test;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.jpa.show-sql=false",
    "codeguard.jwt.secret=test-secret-with-enough-length",
    "codeguard.jwt.expiration-ms=86400000",
    "codeguard.account.expose-dev-verification-code=true",
    "codeguard.account.password-code-ttl-minutes=15"
})
class AuthControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private PasswordEncoder passwordEncoder;

  @Autowired
  private CodeReviewRepository codeReviewRepository;

  @Autowired
  private ProjectRepository projectRepository;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private PasswordChangeVerificationRepository passwordChangeVerificationRepository;

  @BeforeEach
  void setUp() {
    codeReviewRepository.deleteAll();
    projectRepository.deleteAll();
    passwordChangeVerificationRepository.deleteAll();
    userRepository.deleteAll();
  }

  @Test
  void registerCreatesUserAndReturnsToken() throws Exception {
    mockMvc.perform(post("/api/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(new RegisterJson("Amir Cox", "amir@example.com", "password123"))))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.token").isString())
        .andExpect(jsonPath("$.user.id").isNumber())
        .andExpect(jsonPath("$.user.name").value("Amir Cox"))
        .andExpect(jsonPath("$.user.email").value("amir@example.com"));

    UserEntity user = userRepository.findByEmail("amir@example.com").orElseThrow();
    assertThat(user.getPasswordHash()).isNotEqualTo("password123");
    assertThat(passwordEncoder.matches("password123", user.getPasswordHash())).isTrue();
  }

  @Test
  void duplicateRegisterReturnsConflict() throws Exception {
    register("amir@example.com");

    mockMvc.perform(post("/api/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(new RegisterJson("Amir Cox", "amir@example.com", "password123"))))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.message").value("Email is already registered."))
        .andExpect(jsonPath("$.status").value(409))
        .andExpect(jsonPath("$.path").value("/api/auth/register"))
        .andExpect(jsonPath("$.timestamp").isString());
  }

  @Test
  void loginReturnsToken() throws Exception {
    register("amir@example.com");

    mockMvc.perform(post("/api/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(new LoginJson("amir@example.com", "password123"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.token").isString())
        .andExpect(jsonPath("$.user.email").value("amir@example.com"));
  }

  @Test
  void invalidLoginReturnsUnauthorized() throws Exception {
    register("amir@example.com");

    mockMvc.perform(post("/api/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(new LoginJson("amir@example.com", "wrong-password"))))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.message").value("Invalid email or password"))
        .andExpect(jsonPath("$.status").value(401))
        .andExpect(jsonPath("$.path").value("/api/auth/login"))
        .andExpect(jsonPath("$.timestamp").isString());
  }

  @Test
  void registerValidationReturnsCleanErrorShape() throws Exception {
    mockMvc.perform(post("/api/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(new RegisterJson("", "not-an-email", ""))))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").isString())
        .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Name is required")))
        .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Email must be valid")))
        .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Password is required")))
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.path").value("/api/auth/register"))
        .andExpect(jsonPath("$.timestamp").isString());
  }

  @Test
  void protectedEndpointWithoutTokenReturnsCleanUnauthorizedShape() throws Exception {
    mockMvc.perform(get("/api/auth/me"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.message").value("Your session expired. Please log in again."))
        .andExpect(jsonPath("$.status").value(401))
        .andExpect(jsonPath("$.path").value("/api/auth/me"))
        .andExpect(jsonPath("$.timestamp").isString());
  }

  @Test
  void meReturnsCurrentUser() throws Exception {
    String token = register("amir@example.com");

    mockMvc.perform(get("/api/auth/me")
            .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.email").value("amir@example.com"));
  }

  @Test
  void authenticatedUserCanRequestVerifyAndCompletePasswordChange() throws Exception {
    String token = register("amir@example.com");

    String verificationCode = requestPasswordChangeCode(token);

    mockMvc.perform(post("/api/account/password-change/verify")
            .header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(new PasswordVerificationJson(verificationCode))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.message").value("Verification code accepted."));

    mockMvc.perform(post("/api/account/password-change/complete")
            .header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(new CompletePasswordChangeJson(verificationCode, "new-password-123"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.message").value("Password updated. Please sign in again."));

    UserEntity user = userRepository.findByEmail("amir@example.com").orElseThrow();
    assertThat(user.getPasswordHash()).isNotEqualTo("new-password-123");
    assertThat(passwordEncoder.matches("new-password-123", user.getPasswordHash())).isTrue();

    mockMvc.perform(get("/api/auth/me")
            .header("Authorization", "Bearer " + token))
        .andExpect(status().isUnauthorized());

    mockMvc.perform(post("/api/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(new LoginJson("amir@example.com", "password123"))))
        .andExpect(status().isUnauthorized());

    mockMvc.perform(post("/api/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(new LoginJson("amir@example.com", "new-password-123"))))
        .andExpect(status().isOk());
  }

  @Test
  void invalidExpiredOrUsedPasswordVerificationFails() throws Exception {
    String token = register("amir@example.com");
    String verificationCode = requestPasswordChangeCode(token);

    mockMvc.perform(post("/api/account/password-change/verify")
            .header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(new PasswordVerificationJson("000000"))))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("The verification code is invalid, expired, or already used."));

    mockMvc.perform(post("/api/account/password-change/verify")
            .header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(new PasswordVerificationJson(verificationCode))))
        .andExpect(status().isOk());

    mockMvc.perform(post("/api/account/password-change/complete")
            .header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(new CompletePasswordChangeJson(verificationCode, "new-password-123"))))
        .andExpect(status().isOk());

    String newToken = login("amir@example.com", "new-password-123");
    mockMvc.perform(post("/api/account/password-change/complete")
            .header("Authorization", "Bearer " + newToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(new CompletePasswordChangeJson(verificationCode, "another-password-123"))))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("The verification code is invalid, expired, or already used."));
  }

  @Test
  void userCanUpdateEmailAndReceivesFreshToken() throws Exception {
    String token = register("amir@example.com");

    MvcResult result = mockMvc.perform(patch("/api/account/email")
            .header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(new UpdateEmailJson("new-amir@example.com", "password123"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.token").isString())
        .andExpect(jsonPath("$.user.email").value("new-amir@example.com"))
        .andReturn();

    assertThat(userRepository.findActiveByEmail("new-amir@example.com")).isPresent();

    mockMvc.perform(get("/api/auth/me")
            .header("Authorization", "Bearer " + token))
        .andExpect(status().isUnauthorized());

    String newToken = objectMapper.readTree(result.getResponse().getContentAsString()).get("token").asText();
    mockMvc.perform(get("/api/auth/me")
            .header("Authorization", "Bearer " + newToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.email").value("new-amir@example.com"));
  }

  @Test
  void duplicateEmailUpdateFailsAndCannotModifyAnotherAccount() throws Exception {
    String amirToken = register("amir@example.com");
    String taylorToken = register("taylor@example.com");

    mockMvc.perform(patch("/api/account/email")
            .header("Authorization", "Bearer " + amirToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(new UpdateEmailJson("taylor@example.com", "password123"))))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.message").value("Email is already registered."));

    mockMvc.perform(get("/api/auth/me")
            .header("Authorization", "Bearer " + taylorToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.email").value("taylor@example.com"));
  }

  @Test
  void userCanDeleteOwnAccountWithoutDeletingOtherUsers() throws Exception {
    String amirToken = register("amir@example.com");
    String taylorToken = register("taylor@example.com");

    mockMvc.perform(delete("/api/account")
            .header("Authorization", "Bearer " + amirToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(new DeleteAccountJson("password123", "DELETE"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.message").value("Account deleted."));

    UserEntity deletedUser = userRepository.findAll().stream()
        .filter(user -> !user.isEnabled())
        .findFirst()
        .orElseThrow();
    assertThat(deletedUser.isEnabled()).isFalse();
    assertThat(deletedUser.getDeletedAt()).isNotNull();
    assertThat(deletedUser.getEmail()).isNotEqualTo("amir@example.com");

    mockMvc.perform(get("/api/auth/me")
            .header("Authorization", "Bearer " + amirToken))
        .andExpect(status().isUnauthorized());

    mockMvc.perform(post("/api/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(new LoginJson("amir@example.com", "password123"))))
        .andExpect(status().isUnauthorized());

    mockMvc.perform(get("/api/auth/me")
            .header("Authorization", "Bearer " + taylorToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.email").value("taylor@example.com"));
  }

  @Test
  void deletedAccountEmailCanBeRegisteredAgain() throws Exception {
    String token = register("amir@example.com");

    mockMvc.perform(delete("/api/account")
            .header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(new DeleteAccountJson("password123", "DELETE"))))
        .andExpect(status().isOk());

    mockMvc.perform(post("/api/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(new RegisterJson("Amir Cox", "amir@example.com", "password123"))))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.user.email").value("amir@example.com"));

    assertThat(userRepository.findActiveByEmail("amir@example.com")).isPresent();
    assertThat(userRepository.findAll()).hasSize(2);
  }

  private String register(String email) throws Exception {
    MvcResult result = mockMvc.perform(post("/api/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(new RegisterJson("Amir Cox", email, "password123"))))
        .andExpect(status().isCreated())
        .andReturn();
    return objectMapper.readTree(result.getResponse().getContentAsString()).get("token").asText();
  }

  private String login(String email, String password) throws Exception {
    MvcResult result = mockMvc.perform(post("/api/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(new LoginJson(email, password))))
        .andExpect(status().isOk())
        .andReturn();
    return objectMapper.readTree(result.getResponse().getContentAsString()).get("token").asText();
  }

  private String requestPasswordChangeCode(String token) throws Exception {
    MvcResult result = mockMvc.perform(post("/api/account/password-change/request")
            .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.message").value("If this account can change its password, a verification code has been sent."))
        .andExpect(jsonPath("$.devVerificationCode").isString())
        .andReturn();
    return objectMapper.readTree(result.getResponse().getContentAsString()).get("devVerificationCode").asText();
  }

  private record RegisterJson(String name, String email, String password) {
  }

  private record LoginJson(String email, String password) {
  }

  private record PasswordVerificationJson(String verificationCode) {
  }

  private record CompletePasswordChangeJson(String verificationCode, String newPassword) {
  }

  private record UpdateEmailJson(String email, String currentPassword) {
  }

  private record DeleteAccountJson(String currentPassword, String confirmation) {
  }
}
