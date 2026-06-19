package com.codeguard.backend.user.repository;

import com.codeguard.backend.user.entity.UserEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<UserEntity, Long> {

  Optional<UserEntity> findByEmail(String email);

  boolean existsByEmail(String email);
}
