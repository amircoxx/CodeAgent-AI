package com.codeguard.backend.auth;

import com.codeguard.backend.user.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  private final JwtService jwtService;
  private final UserRepository userRepository;

  public JwtAuthenticationFilter(JwtService jwtService, UserRepository userRepository) {
    this.jwtService = jwtService;
    this.userRepository = userRepository;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request,
      HttpServletResponse response,
      FilterChain filterChain
  ) throws ServletException, IOException {
    String authorizationHeader = request.getHeader("Authorization");

    if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
      String token = authorizationHeader.substring("Bearer ".length());
      if (jwtService.isTokenValid(token)) {
        String email = jwtService.extractSubject(token);
        int tokenVersion = jwtService.extractTokenVersion(token);
        userRepository.findActiveByEmail(email).filter(user -> user.getTokenVersion() == tokenVersion).ifPresent(user -> {
          UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
              user.getEmail(),
              null,
              List.of(new SimpleGrantedAuthority("ROLE_USER"))
          );
          SecurityContextHolder.getContext().setAuthentication(authentication);
        });
      }
    }

    filterChain.doFilter(request, response);
  }
}
