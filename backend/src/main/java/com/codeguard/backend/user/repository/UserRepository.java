package com.codeguard.backend.user.repository;

import com.codeguard.backend.user.entity.UserEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface UserRepository extends JpaRepository<UserEntity, Long> {

  Optional<UserEntity> findByEmail(String email);

  @Query("""
      select user from UserEntity user
      where user.email = :email
        and (user.enabled is null or user.enabled = true)
        and user.deletedAt is null
      """)
  Optional<UserEntity> findActiveByEmail(String email);

  boolean existsByEmail(String email);

  @Query("""
      select count(user) > 0 from UserEntity user
      where user.email = :email
        and (user.enabled is null or user.enabled = true)
        and user.deletedAt is null
      """)
  boolean existsActiveByEmail(String email);
}
