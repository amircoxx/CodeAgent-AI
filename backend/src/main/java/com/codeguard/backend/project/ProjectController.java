package com.codeguard.backend.project;

import com.codeguard.backend.project.dto.ProjectRequest;
import com.codeguard.backend.project.dto.ProjectResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/projects")
public class ProjectController {

  private final ProjectService projectService;

  public ProjectController(ProjectService projectService) {
    this.projectService = projectService;
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public ProjectResponse createProject(@Valid @RequestBody ProjectRequest request) {
    return projectService.createProject(request);
  }

  @GetMapping
  public List<ProjectResponse> getProjects() {
    return projectService.getAllProjects();
  }

  @GetMapping("/{id}")
  public ProjectResponse getProject(@PathVariable Long id) {
    return projectService.getProjectById(id);
  }
}
