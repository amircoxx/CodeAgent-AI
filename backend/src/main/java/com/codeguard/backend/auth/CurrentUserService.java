package com.codeguard.backend.auth;

import com.codeguard.backend.user.entity.UserEntity;
import com.codeguard.backend.user.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class CurrentUserService {

  private final UserRepository userRepository;

  public CurrentUserService(UserRepository userRepository) {
    this.userRepository = userRepository;
  }

  public UserEntity getCurrentUser() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null || !authentication.isAuthenticated()) {
      throw new InvalidCredentialsException();
    }

    return userRepository.findActiveByEmail(authentication.getName())
        .orElseThrow(InvalidCredentialsException::new);
  }
}
