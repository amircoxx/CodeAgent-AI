package com.codeguard.backend.github.entity;

import com.codeguard.backend.user.entity.UserEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;

@Entity
@Table(
    name = "github_connections",
    uniqueConstraints = @UniqueConstraint(name = "uk_github_connections_user", columnNames = "user_id")
)
public class GitHubConnectionEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false)
  private UserEntity user;

  @Column(nullable = false)
  private Long installationId;

  @Column(nullable = false)
  private String accountLogin;

  @Column(nullable = false)
  private String accountType;

  @Column(nullable = false, updatable = false)
  private Instant createdAt;

  @Column(nullable = false)
  private Instant updatedAt;

  protected GitHubConnectionEntity() {
  }

  public GitHubConnectionEntity(
      UserEntity user,
      Long installationId,
      String accountLogin,
      String accountType
  ) {
    this.user = user;
    this.installationId = installationId;
    this.accountLogin = accountLogin;
    this.accountType = accountType;
  }

  @PrePersist
  void onCreate() {
    Instant now = Instant.now();
    createdAt = now;
    updatedAt = now;
  }

  @PreUpdate
  void onUpdate() {
    updatedAt = Instant.now();
  }

  public Long getId() {
    return id;
  }

  public UserEntity getUser() {
    return user;
  }

  public Long getInstallationId() {
    return installationId;
  }

  public String getAccountLogin() {
    return accountLogin;
  }

  public String getAccountType() {
    return accountType;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public void updateInstallation(Long installationId, String accountLogin, String accountType) {
    this.installationId = installationId;
    this.accountLogin = accountLogin;
    this.accountType = accountType;
  }
}
