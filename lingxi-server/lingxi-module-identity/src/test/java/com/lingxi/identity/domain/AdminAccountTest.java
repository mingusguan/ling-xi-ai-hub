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

  /** 密码错误提示必须带剩余次数，否则用户会在不知情的情况下把账号打到锁定。 */
  @Test
  void loginFailureMessageShouldReportRemainingAttempts() {
    LocalDateTime now = LocalDateTime.of(2026, 8, 10, 10, 0);
    AdminAccount account = AdminAccount.create(1L, "admin", "管理员", "encoded", now);

    assertThat(account.remainingAttempts()).isEqualTo(5);

    account.loginFailed(now);
    assertThat(account.remainingAttempts()).isEqualTo(4);
    assertThat(account.loginFailedMessage()).contains("还可尝试 4 次").contains("15 分钟");

    account.loginFailed(now.plusMinutes(1));
    account.loginFailed(now.plusMinutes(2));
    account.loginFailed(now.plusMinutes(3));
    assertThat(account.remainingAttempts()).isEqualTo(1);
    assertThat(account.loginFailedMessage()).contains("还可尝试 1 次");

    // 第 5 次失败即锁定：提示改为“已临时锁定”，且不再给出可尝试次数。
    account.loginFailed(now.plusMinutes(4));
    assertThat(account.remainingAttempts()).isZero();
    assertThat(account.loginFailedMessage()).contains("已临时锁定").doesNotContain("还可尝试");
  }

  /** 锁定中的提示必须给出剩余分钟数，且不会出现“请 0 分钟后再试”。 */
  @Test
  void lockedMessageShouldReportRemainingMinutes() {
    LocalDateTime now = LocalDateTime.of(2026, 8, 10, 10, 0);
    AdminAccount account = AdminAccount.create(1L, "admin", "管理员", "encoded", now);
    for (int i = 0; i < 5; i++) {
      account.loginFailed(now.plusMinutes(i));
    }
    LocalDateTime lockedUntil = now.plusMinutes(4).plusMinutes(15);

    // 刚锁定：剩余 15 分钟，向上取整不应变成 14 或 0。
    assertThatThrownBy(() -> account.ensureLoginAllowed(now.plusMinutes(4)))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("请 15 分钟后再试");

    // 锁定即将到期（剩余不足 1 分钟）时仍提示 1 分钟，而不是 0 分钟。
    assertThatThrownBy(() -> account.ensureLoginAllowed(lockedUntil.minusSeconds(30)))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("请 1 分钟后再试");

    // 锁定到期后放行。
    account.ensureLoginAllowed(lockedUntil);
    assertThat(account.getLockedUntil()).isEqualTo(lockedUntil);
  }
}
