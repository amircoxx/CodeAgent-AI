package com.codeguard.backend.account.repository;

import com.codeguard.backend.account.entity.PasswordChangeVerificationEntity;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PasswordChangeVerificationRepository extends JpaRepository<PasswordChangeVerificationEntity, Long> {

  Optional<PasswordChangeVerificationEntity> findByUserIdAndTokenHash(Long userId, String tokenHash);

  @Modifying
  @Query("""
      update PasswordChangeVerificationEntity verification
      set verification.consumedAt = :consumedAt
      where verification.user.id = :userId
        and verification.consumedAt is null
      """)
  void consumeOutstandingForUser(@Param("userId") Long userId, @Param("consumedAt") Instant consumedAt);
}
