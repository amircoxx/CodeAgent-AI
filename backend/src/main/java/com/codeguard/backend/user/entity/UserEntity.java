package com.codeguard.backend.user.entity;

import com.codeguard.backend.project.entity.ProjectEntity;
import com.codeguard.backend.review.entity.CodeReviewEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "users")
public class UserEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private String name;

  @Column(nullable = false, unique = true)
  private String email;

  @Column(nullable = false)
  private String passwordHash;

  @Column
  private Boolean enabled;

  @Column
  private Integer tokenVersion;

  @Column
  private Instant deletedAt;

  @Column(nullable = false, updatable = false)
  private Instant createdAt;

  @Column(nullable = false)
  private Instant updatedAt;

  @OneToMany(mappedBy = "user", cascade = CascadeType.PERSIST)
  private List<ProjectEntity> projects = new ArrayList<>();

  @OneToMany(mappedBy = "user", cascade = CascadeType.PERSIST)
  private List<CodeReviewEntity> reviews = new ArrayList<>();

  protected UserEntity() {
  }

  public UserEntity(String name, String email, String passwordHash) {
    this.name = name;
    this.email = email;
    this.passwordHash = passwordHash;
    this.enabled = true;
    this.tokenVersion = 0;
  }

  @PrePersist
  void onCreate() {
    Instant now = Instant.now();
    createdAt = now;
    updatedAt = now;
    if (enabled == null) {
      enabled = true;
    }
    if (tokenVersion == null) {
      tokenVersion = 0;
    }
  }

  @PreUpdate
  void onUpdate() {
    updatedAt = Instant.now();
  }

  public Long getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public String getEmail() {
    return email;
  }

  public String getPasswordHash() {
    return passwordHash;
  }

  public boolean isEnabled() {
    return enabled == null || enabled;
  }

  public int getTokenVersion() {
    return tokenVersion == null ? 0 : tokenVersion;
  }

  public Instant getDeletedAt() {
    return deletedAt;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public List<ProjectEntity> getProjects() {
    return projects;
  }

  public List<CodeReviewEntity> getReviews() {
    return reviews;
  }

  public void updatePasswordHash(String passwordHash) {
    this.passwordHash = passwordHash;
  }

  public void updateEmail(String email) {
    this.email = email;
  }

  public void incrementTokenVersion() {
    tokenVersion = getTokenVersion() + 1;
  }

  public void softDelete() {
    enabled = false;
    deletedAt = Instant.now();
    incrementTokenVersion();
  }
}
