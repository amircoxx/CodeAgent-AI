package com.codeguard.backend.account;

import com.codeguard.backend.user.entity.UserEntity;
import org.springframework.stereotype.Component;

@Component
public class NoopAccountVerificationEmailSender implements AccountVerificationEmailSender {

  @Override
  public void sendPasswordChangeCode(UserEntity user, String verificationCode) {
    // Email delivery can be wired here once an SMTP or transactional-email provider is configured.
  }
}
