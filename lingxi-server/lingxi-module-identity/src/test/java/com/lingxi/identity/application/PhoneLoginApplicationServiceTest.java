package com.lingxi.identity.application;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.lingxi.identity.api.PhoneLoginCommand;
import com.lingxi.identity.api.RegisterVerifiedUserCommand;
import com.lingxi.identity.api.RegisteredUserResult;
import com.lingxi.identity.api.SessionTokens;
import com.lingxi.identity.domain.IdentitySecurityRepository;
import com.lingxi.identity.domain.LoginIdentity;
import com.lingxi.kernel.BusinessException;
import com.lingxi.kernel.IdGenerator;
import java.time.*;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class PhoneLoginApplicationServiceTest {
  private static final LocalDate BIRTH_DATE = LocalDate.of(1990, 1, 1);
  private static final String DEVICE_ID = "device-1";
  private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-09-12T04:00:00Z"), ZoneOffset.UTC);

  private IdentitySecurityRepository repository;
  private IdentityApplicationService identityService;
  private AuthenticationApplicationService authenticationService;
  private IdGenerator idGenerator;

  @BeforeEach
  void setUp() {
    repository = mock(IdentitySecurityRepository.class);
    identityService = mock(IdentityApplicationService.class);
    authenticationService = mock(AuthenticationApplicationService.class);
    idGenerator = mock(IdGenerator.class);
    when(idGenerator.nextId()).thenReturn(7001L);
  }

  private PhoneLoginApplicationService service(boolean enabled) {
    return new PhoneLoginApplicationService(
        repository, identityService, authenticationService, idGenerator, enabled, CLOCK);
  }

  @Test
  void existingPhoneIdentityIssuesSessionWithoutRegistration() {
    LocalDateTime now = LocalDateTime.of(2026, 9, 12, 4, 0);
    when(repository.findLoginIdentity(eq("PHONE"), anyString()))
        .thenReturn(Optional.of(new LoginIdentity(1L, 42L, "PHONE", "hash", now, now)));
    when(authenticationService.issueSession(42L, DEVICE_ID)).thenReturn(tokens());

    SessionTokens result =
        service(true)
            .loginWithPhone(new PhoneLoginCommand("+86 138-0000-0001", null, DEVICE_ID, "Asia/Shanghai"));

    assertThat(result.accessToken()).isEqualTo("access");
    verify(identityService, never()).registerVerifiedUser(any());
  }

  @Test
  void firstLoginRegistersUserAndBindsPhoneIdentity() {
    when(repository.findLoginIdentity(eq("PHONE"), anyString())).thenReturn(Optional.empty());
    when(identityService.registerVerifiedUser(any()))
        .thenReturn(new RegisteredUserResult(42L, "U-42", com.lingxi.identity.api.AgeBand.ADULT,
            com.lingxi.identity.api.AccountStatus.ACTIVE_ADULT, 1L));
    when(authenticationService.issueSession(42L, DEVICE_ID)).thenReturn(tokens());

    service(true)
        .loginWithPhone(new PhoneLoginCommand("13800000002", BIRTH_DATE, DEVICE_ID, "Asia/Shanghai"));

    ArgumentCaptor<RegisterVerifiedUserCommand> registration =
        ArgumentCaptor.forClass(RegisterVerifiedUserCommand.class);
    verify(identityService).registerVerifiedUser(registration.capture());
    assertThat(registration.getValue().verificationMethod()).isEqualTo("PHONE_NUMBER");
    assertThat(registration.getValue().evidenceReference()).isEqualTo("phone:****0002");
    ArgumentCaptor<LoginIdentity> identity = ArgumentCaptor.forClass(LoginIdentity.class);
    verify(repository).insertLoginIdentity(identity.capture());
    assertThat(identity.getValue().channel()).isEqualTo("PHONE");
    // 数据库只保存手机号摘要，不保存明文号码。
    assertThat(identity.getValue().subjectHash()).doesNotContain("13800000002");
  }

  @Test
  void firstLoginWithoutBirthDateIsRejected() {
    when(repository.findLoginIdentity(eq("PHONE"), anyString())).thenReturn(Optional.empty());

    assertThatThrownBy(
            () ->
                service(true)
                    .loginWithPhone(new PhoneLoginCommand("13800000003", null, DEVICE_ID, null)))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("首次使用手机号登录需要提供出生日期");
    verify(identityService, never()).registerVerifiedUser(any());
  }

  @Test
  void invalidPhoneNumberIsRejected() {
    assertThatThrownBy(
            () ->
                service(true)
                    .loginWithPhone(new PhoneLoginCommand("12345", BIRTH_DATE, DEVICE_ID, null)))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("手机号格式不正确");
  }

  @Test
  void disabledSwitchBlocksPhoneLogin() {
    assertThatThrownBy(
            () ->
                service(false)
                    .loginWithPhone(
                        new PhoneLoginCommand("13800000004", BIRTH_DATE, DEVICE_ID, null)))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("手机号登录入口已关闭");
    verify(authenticationService, never()).issueSession(anyLong(), anyString());
  }

  private SessionTokens tokens() {
    return new SessionTokens(
        "access",
        "refresh",
        "family",
        Instant.parse("2026-09-12T04:15:00Z"),
        Instant.parse("2026-10-12T04:00:00Z"),
        com.lingxi.identity.api.AgeBand.ADULT,
        com.lingxi.identity.api.AccountStatus.ACTIVE_ADULT,
        1L);
  }
}
