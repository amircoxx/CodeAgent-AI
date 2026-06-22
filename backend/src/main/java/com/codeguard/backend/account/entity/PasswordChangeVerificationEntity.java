package com.codeguard.backend.account.entity;

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
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "password_change_verifications")
public class PasswordChangeVerificationEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false)
  private UserEntity user;

  @Column(nullable = false, length = 64)
  private String tokenHash;

  @Column(nullable = false)
  private Instant expiresAt;

  @Column(nullable = false, updatable = false)
  private Instant createdAt;

  @Column
  private Instant verifiedAt;

  @Column
  private Instant consumedAt;

  protected PasswordChangeVerificationEntity() {
  }

  public PasswordChangeVerificationEntity(UserEntity user, String tokenHash, Instant expiresAt) {
    this.user = user;
    this.tokenHash = tokenHash;
    this.expiresAt = expiresAt;
  }

  @PrePersist
  void onCreate() {
    createdAt = Instant.now();
  }

  public Long getId() {
    return id;
  }

  public UserEntity getUser() {
    return user;
  }

  public String getTokenHash() {
    return tokenHash;
  }

  public Instant getExpiresAt() {
    return expiresAt;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getVerifiedAt() {
    return verifiedAt;
  }

  public Instant getConsumedAt() {
    return consumedAt;
  }

  public boolean isExpired(Instant now) {
    return !expiresAt.isAfter(now);
  }

  public boolean isConsumed() {
    return consumedAt != null;
  }

  public boolean isVerified() {
    return verifiedAt != null;
  }

  public void markVerified() {
    verifiedAt = Instant.now();
  }

  public void markConsumed() {
    consumedAt = Instant.now();
  }
}
