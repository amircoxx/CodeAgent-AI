package com.codeguard.backend.account;

import com.codeguard.backend.user.entity.UserEntity;

public interface AccountVerificationEmailSender {

  void sendPasswordChangeCode(UserEntity user, String verificationCode);
}
