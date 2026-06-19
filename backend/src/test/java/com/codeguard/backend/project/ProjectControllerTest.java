package com.codeguard.backend.project;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.codeguard.backend.project.repository.ProjectRepository;
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
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:codeguard-project-test;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.jpa.show-sql=false"
})
class ProjectControllerTest {

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

  @BeforeEach
  void setUp() {
    codeReviewRepository.deleteAll();
    projectRepository.deleteAll();
    userRepository.deleteAll();
  }

  @Test
  void createProjectCreatesProject() throws Exception {
    String token = register("amir@example.com");

    mockMvc.perform(post("/api/projects")
            .header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content(projectJson("CodeGuard Backend", "Spring Boot backend code review project")))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").isNumber())
        .andExpect(jsonPath("$.name").value("CodeGuard Backend"))
        .andExpect(jsonPath("$.description").value("Spring Boot backend code review project"))
        .andExpect(jsonPath("$.createdAt").isString())
        .andExpect(jsonPath("$.updatedAt").isString());
  }

  @Test
  void getProjectsReturnsProjectsNewestFirst() throws Exception {
    String token = register("amir@example.com");
    createProject(token, "Older Project", "First project");
    createProject(token, "Newer Project", "Second project");

    mockMvc.perform(get("/api/projects")
            .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].name").value("Newer Project"))
        .andExpect(jsonPath("$[1].name").value("Older Project"));
  }

  @Test
  void getProjectReturnsProject() throws Exception {
    String token = register("amir@example.com");
    MvcResult created = createProject(token, "CodeGuard Frontend", "Next.js frontend");
    long id = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asLong();

    mockMvc.perform(get("/api/projects/{id}", id)
            .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(id))
        .andExpect(jsonPath("$.name").value("CodeGuard Frontend"))
        .andExpect(jsonPath("$.description").value("Next.js frontend"));
  }

  @Test
  void getProjectReturnsNotFoundForMissingProject() throws Exception {
    String token = register("amir@example.com");

    mockMvc.perform(get("/api/projects/{id}", 999L)
            .header("Authorization", "Bearer " + token))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.message").value("Project not found: 999"))
        .andExpect(jsonPath("$.status").value(404))
        .andExpect(jsonPath("$.path").value("/api/projects/999"))
        .andExpect(jsonPath("$.timestamp").isString());
  }

  @Test
  void protectedProjectEndpointsRequireAuth() throws Exception {
    mockMvc.perform(get("/api/projects"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.message").value("Your session expired. Please log in again."))
        .andExpect(jsonPath("$.status").value(401))
        .andExpect(jsonPath("$.path").value("/api/projects"))
        .andExpect(jsonPath("$.timestamp").isString());
  }

  @Test
  void createProjectValidationReturnsCleanErrorShape() throws Exception {
    String token = register("amir@example.com");

    mockMvc.perform(post("/api/projects")
            .header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content(projectJson("", "")))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Project name is required")))
        .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Project description is required")))
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.path").value("/api/projects"))
        .andExpect(jsonPath("$.timestamp").isString());
  }

  @Test
  void getProjectsReturnsOnlyCurrentUsersProjects() throws Exception {
    String amirToken = register("amir@example.com");
    String taylorToken = register("taylor@example.com");
    createProject(amirToken, "Amir Project", "Owned by Amir");
    createProject(taylorToken, "Taylor Project", "Owned by Taylor");

    mockMvc.perform(get("/api/projects")
            .header("Authorization", "Bearer " + amirToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].name").value("Amir Project"));
  }

  @Test
  void getProjectReturnsNotFoundForAnotherUsersProject() throws Exception {
    String amirToken = register("amir@example.com");
    String taylorToken = register("taylor@example.com");
    MvcResult created = createProject(taylorToken, "Taylor Project", "Owned by Taylor");
    long id = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asLong();

    mockMvc.perform(get("/api/projects/{id}", id)
            .header("Authorization", "Bearer " + amirToken))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.message").value("Project not found: " + id))
        .andExpect(jsonPath("$.status").value(404));
  }

  private MvcResult createProject(String token, String name, String description) throws Exception {
    return mockMvc.perform(post("/api/projects")
            .header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content(projectJson(name, description)))
        .andExpect(status().isCreated())
        .andReturn();
  }

  private String register(String email) throws Exception {
    MvcResult result = mockMvc.perform(post("/api/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(new RegisterRequest("Amir Cox", email, "password123"))))
        .andExpect(status().isCreated())
        .andReturn();
    return objectMapper.readTree(result.getResponse().getContentAsString()).get("token").asText();
  }

  private String projectJson(String name, String description) throws Exception {
    return objectMapper.writeValueAsString(new CreateProjectRequest(name, description));
  }

  private record CreateProjectRequest(String name, String description) {
  }

  private record RegisterRequest(String name, String email, String password) {
  }
}
