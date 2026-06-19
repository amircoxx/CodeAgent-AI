package com.codeguard.backend.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
    "codeguard.jwt.expiration-ms=86400000"
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

  @BeforeEach
  void setUp() {
    codeReviewRepository.deleteAll();
    projectRepository.deleteAll();
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

  private String register(String email) throws Exception {
    MvcResult result = mockMvc.perform(post("/api/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(new RegisterJson("Amir Cox", email, "password123"))))
        .andExpect(status().isCreated())
        .andReturn();
    return objectMapper.readTree(result.getResponse().getContentAsString()).get("token").asText();
  }

  private record RegisterJson(String name, String email, String password) {
  }

  private record LoginJson(String email, String password) {
  }
}
