package com.codeguard.backend.auth;

import com.codeguard.backend.auth.dto.AuthResponse;
import com.codeguard.backend.auth.dto.LoginRequest;
import com.codeguard.backend.auth.dto.RegisterRequest;
import com.codeguard.backend.user.dto.UserResponse;
import com.codeguard.backend.user.entity.UserEntity;
import com.codeguard.backend.user.repository.UserRepository;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final JwtService jwtService;
  private final CurrentUserService currentUserService;

  public AuthService(
      UserRepository userRepository,
      PasswordEncoder passwordEncoder,
      JwtService jwtService,
      CurrentUserService currentUserService
  ) {
    this.userRepository = userRepository;
    this.passwordEncoder = passwordEncoder;
    this.jwtService = jwtService;
    this.currentUserService = currentUserService;
  }

  @Transactional
  public AuthResponse register(RegisterRequest request) {
    String email = normalizeEmail(request.email());
    if (userRepository.existsActiveByEmail(email)) {
      throw new DuplicateEmailException(email);
    }
    userRepository.findByEmail(email)
        .filter(user -> !user.isEnabled() || user.getDeletedAt() != null)
        .ifPresent(user -> {
          user.updateEmail(deletedEmailAlias(user));
          userRepository.saveAndFlush(user);
        });

    UserEntity user = userRepository.save(new UserEntity(
        request.name().trim(),
        email,
        passwordEncoder.encode(request.password())
    ));
    return toAuthResponse(user);
  }

  @Transactional(readOnly = true)
  public AuthResponse login(LoginRequest request) {
    String email = normalizeEmail(request.email());
    UserEntity user = userRepository.findActiveByEmail(email)
        .orElseThrow(InvalidCredentialsException::new);
    if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
      throw new InvalidCredentialsException();
    }

    return toAuthResponse(user);
  }

  @Transactional(readOnly = true)
  public UserResponse getCurrentUser() {
    return toUserResponse(currentUserService.getCurrentUser());
  }

  private AuthResponse toAuthResponse(UserEntity user) {
    return new AuthResponse(jwtService.generateToken(user), toUserResponse(user));
  }

  private UserResponse toUserResponse(UserEntity user) {
    return new UserResponse(user.getId(), user.getName(), user.getEmail());
  }

  private String normalizeEmail(String email) {
    return email.trim().toLowerCase();
  }

  private String deletedEmailAlias(UserEntity user) {
    return "deleted-" + user.getId() + "-" + UUID.randomUUID() + "@deleted.local";
  }
}
