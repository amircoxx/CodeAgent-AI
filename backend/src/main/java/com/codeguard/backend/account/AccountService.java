package com.codeguard.backend.account;

import com.codeguard.backend.account.dto.CompletePasswordChangeRequest;
import com.codeguard.backend.account.dto.DeleteAccountRequest;
import com.codeguard.backend.account.dto.MessageResponse;
import com.codeguard.backend.account.dto.PasswordChangeRequestResponse;
import com.codeguard.backend.account.dto.PasswordVerificationRequest;
import com.codeguard.backend.account.dto.UpdateEmailRequest;
import com.codeguard.backend.account.entity.PasswordChangeVerificationEntity;
import com.codeguard.backend.account.repository.PasswordChangeVerificationRepository;
import com.codeguard.backend.auth.CurrentUserService;
import com.codeguard.backend.auth.DuplicateEmailException;
import com.codeguard.backend.auth.InvalidCredentialsException;
import com.codeguard.backend.auth.JwtService;
import com.codeguard.backend.auth.dto.AuthResponse;
import com.codeguard.backend.shared.config.CodeGuardAccountProperties;
import com.codeguard.backend.user.dto.UserResponse;
import com.codeguard.backend.user.entity.UserEntity;
import com.codeguard.backend.user.repository.UserRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountService {

  private static final String PASSWORD_CHANGE_REQUESTED =
      "If this account can change its password, a verification code has been sent.";
  private static final String PASSWORD_VERIFIED = "Verification code accepted.";
  private static final String PASSWORD_CHANGED = "Password updated. Please sign in again.";
  private static final String ACCOUNT_DELETED = "Account deleted.";
  private static final int VERIFICATION_TOKEN_BYTES = 32;

  private final CurrentUserService currentUserService;
  private final UserRepository userRepository;
  private final PasswordChangeVerificationRepository verificationRepository;
  private final PasswordEncoder passwordEncoder;
  private final JwtService jwtService;
  private final CodeGuardAccountProperties accountProperties;
  private final AccountVerificationEmailSender emailSender;
  private final SecureRandom secureRandom = new SecureRandom();

  public AccountService(
      CurrentUserService currentUserService,
      UserRepository userRepository,
      PasswordChangeVerificationRepository verificationRepository,
      PasswordEncoder passwordEncoder,
      JwtService jwtService,
      CodeGuardAccountProperties accountProperties,
      AccountVerificationEmailSender emailSender
  ) {
    this.currentUserService = currentUserService;
    this.userRepository = userRepository;
    this.verificationRepository = verificationRepository;
    this.passwordEncoder = passwordEncoder;
    this.jwtService = jwtService;
    this.accountProperties = accountProperties;
    this.emailSender = emailSender;
  }

  @Transactional
  public PasswordChangeRequestResponse requestPasswordChange() {
    UserEntity user = currentUserService.getCurrentUser();
    verificationRepository.consumeOutstandingForUser(user.getId(), Instant.now());

    String verificationCode = generateVerificationCode();
    PasswordChangeVerificationEntity verification = new PasswordChangeVerificationEntity(
        user,
        hashToken(verificationCode),
        Instant.now().plus(accountProperties.passwordCodeTtl())
    );
    verificationRepository.save(verification);
    emailSender.sendPasswordChangeCode(user, verificationCode);

    return new PasswordChangeRequestResponse(
        PASSWORD_CHANGE_REQUESTED,
        accountProperties.exposeDevVerificationCode() ? verificationCode : null
    );
  }

  @Transactional
  public MessageResponse verifyPasswordChange(PasswordVerificationRequest request) {
    UserEntity user = currentUserService.getCurrentUser();
    PasswordChangeVerificationEntity verification = requireUsableVerification(user, request.verificationCode());
    verification.markVerified();
    return new MessageResponse(PASSWORD_VERIFIED);
  }

  @Transactional
  public MessageResponse completePasswordChange(CompletePasswordChangeRequest request) {
    UserEntity user = currentUserService.getCurrentUser();
    PasswordChangeVerificationEntity verification = requireUsableVerification(user, request.verificationCode());
    if (!verification.isVerified()) {
      throw new InvalidPasswordVerificationException();
    }

    user.updatePasswordHash(passwordEncoder.encode(request.newPassword()));
    user.incrementTokenVersion();
    verification.markConsumed();
    userRepository.save(user);
    return new MessageResponse(PASSWORD_CHANGED);
  }

  @Transactional
  public AuthResponse updateEmail(UpdateEmailRequest request) {
    UserEntity user = currentUserService.getCurrentUser();
    requireCurrentPassword(user, request.currentPassword());

    String email = normalizeEmail(request.email());
    if (!email.equals(user.getEmail()) && userRepository.existsActiveByEmail(email)) {
      throw new DuplicateEmailException(email);
    }

    user.updateEmail(email);
    user.incrementTokenVersion();
    UserEntity savedUser = userRepository.save(user);
    return new AuthResponse(jwtService.generateToken(savedUser), toUserResponse(savedUser));
  }

  @Transactional
  public MessageResponse deleteAccount(DeleteAccountRequest request) {
    UserEntity user = currentUserService.getCurrentUser();
    requireCurrentPassword(user, request.currentPassword());
    if (!"DELETE".equals(request.confirmation())) {
      throw new InvalidAccountConfirmationException();
    }

    verificationRepository.consumeOutstandingForUser(user.getId(), Instant.now());
    user.updateEmail(deletedEmailAlias(user));
    user.softDelete();
    userRepository.save(user);
    return new MessageResponse(ACCOUNT_DELETED);
  }

  private PasswordChangeVerificationEntity requireUsableVerification(UserEntity user, String verificationCode) {
    PasswordChangeVerificationEntity verification = verificationRepository
        .findByUserIdAndTokenHash(user.getId(), hashToken(verificationCode))
        .orElseThrow(InvalidPasswordVerificationException::new);

    if (verification.isConsumed() || verification.isExpired(Instant.now())) {
      throw new InvalidPasswordVerificationException();
    }

    return verification;
  }

  private void requireCurrentPassword(UserEntity user, String currentPassword) {
    if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
      throw new InvalidCredentialsException();
    }
  }

  private String generateVerificationCode() {
    byte[] token = new byte[VERIFICATION_TOKEN_BYTES];
    secureRandom.nextBytes(token);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(token);
  }

  private String hashToken(String token) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hash = digest.digest(token.trim().getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(hash);
    } catch (Exception exception) {
      throw new IllegalStateException("Could not hash verification token", exception);
    }
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
