package com.lingxi.identity.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lingxi.kernel.BusinessException;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class AdminAccountTest {

  @Test
  void shouldLockAccountAfterFiveFailedLoginsAndUnlockAfterSuccessfulLogin() {
    LocalDateTime now = LocalDateTime.of(2026, 8, 10, 10, 0);
    AdminAccount account = AdminAccount.create(1L, "admin.ops", "运营管理员", "encoded", now);

    for (int i = 0; i < 5; i++) {
      account.loginFailed(now.plusMinutes(i));
    }

    assertThat(account.getFailedAttempts()).isEqualTo(5);
    assertThat(account.getLockedUntil()).isEqualTo(now.plusMinutes(19));
    assertThatThrownBy(() -> account.ensureLoginAllowed(now.plusMinutes(10)))
        .isInstanceOf(BusinessException.class)
        .extracting("code")
        .isEqualTo("ADMIN_ACCOUNT_LOCKED");

    account.loginSucceeded(now.plusMinutes(20));

    assertThat(account.getFailedAttempts()).isZero();
    assertThat(account.getLockedUntil()).isNull();
    assertThat(account.getLastLoginAt()).isEqualTo(now.plusMinutes(20));
  }

  @Test
  void shouldRejectInvalidUsernameAndUnsupportedStatus() {
    LocalDateTime now = LocalDateTime.of(2026, 8, 10, 10, 0);

    assertThatThrownBy(() -> AdminAccount.create(1L, "a", "管理员", "encoded", now))
        .isInstanceOf(BusinessException.class);

    AdminAccount account = AdminAccount.create(1L, "admin", "管理员", "encoded", now);
    assertThatThrownBy(() -> account.changeStatus("DELETED", now))
        .isInstanceOf(BusinessException.class)
        .extracting("code")
        .isEqualTo("ADMIN_ACCOUNT_STATUS_INVALID");
  }
}
