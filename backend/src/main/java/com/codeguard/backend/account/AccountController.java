package com.codeguard.backend.account;

import com.codeguard.backend.account.dto.CompletePasswordChangeRequest;
import com.codeguard.backend.account.dto.DeleteAccountRequest;
import com.codeguard.backend.account.dto.MessageResponse;
import com.codeguard.backend.account.dto.PasswordChangeRequestResponse;
import com.codeguard.backend.account.dto.PasswordVerificationRequest;
import com.codeguard.backend.account.dto.UpdateEmailRequest;
import com.codeguard.backend.auth.dto.AuthResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/account")
public class AccountController {

  private final AccountService accountService;

  public AccountController(AccountService accountService) {
    this.accountService = accountService;
  }

  @PostMapping("/password-change/request")
  public PasswordChangeRequestResponse requestPasswordChange() {
    return accountService.requestPasswordChange();
  }

  @PostMapping("/password-change/verify")
  public MessageResponse verifyPasswordChange(@Valid @RequestBody PasswordVerificationRequest request) {
    return accountService.verifyPasswordChange(request);
  }

  @PostMapping("/password-change/complete")
  public MessageResponse completePasswordChange(@Valid @RequestBody CompletePasswordChangeRequest request) {
    return accountService.completePasswordChange(request);
  }

  @PatchMapping("/email")
  public AuthResponse updateEmail(@Valid @RequestBody UpdateEmailRequest request) {
    return accountService.updateEmail(request);
  }

  @DeleteMapping
  public MessageResponse deleteAccount(@Valid @RequestBody DeleteAccountRequest request) {
    return accountService.deleteAccount(request);
  }
}
