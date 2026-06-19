package com.codeguard.backend.project;

import com.codeguard.backend.auth.CurrentUserService;
import com.codeguard.backend.project.dto.ProjectRequest;
import com.codeguard.backend.project.dto.ProjectResponse;
import com.codeguard.backend.project.entity.ProjectEntity;
import com.codeguard.backend.project.repository.ProjectRepository;
import com.codeguard.backend.user.entity.UserEntity;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProjectService {

  private final ProjectRepository projectRepository;
  private final CurrentUserService currentUserService;

  public ProjectService(ProjectRepository projectRepository, CurrentUserService currentUserService) {
    this.projectRepository = projectRepository;
    this.currentUserService = currentUserService;
  }

  @Transactional
  public ProjectResponse createProject(ProjectRequest request) {
    UserEntity user = currentUserService.getCurrentUser();
    ProjectEntity project = new ProjectEntity(
        request.name().trim(),
        request.description().trim()
    );
    project.setUser(user);

    return toResponse(projectRepository.save(project));
  }

  @Transactional(readOnly = true)
  public List<ProjectResponse> getAllProjects() {
    UserEntity user = currentUserService.getCurrentUser();
    return projectRepository.findAllByUserIdOrderByCreatedAtDesc(user.getId())
        .stream()
        .map(this::toResponse)
        .toList();
  }

  @Transactional(readOnly = true)
  public ProjectResponse getProjectById(Long id) {
    UserEntity user = currentUserService.getCurrentUser();
    ProjectEntity project = projectRepository.findByIdAndUserId(id, user.getId())
        .orElseThrow(() -> new ProjectNotFoundException(id));
    return toResponse(project);
  }

  private ProjectResponse toResponse(ProjectEntity project) {
    return new ProjectResponse(
        project.getId(),
        project.getName(),
        project.getDescription(),
        project.getCreatedAt(),
        project.getUpdatedAt()
    );
  }
}
