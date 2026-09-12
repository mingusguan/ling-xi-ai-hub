package com.lingxi.identity.interfaces;

import com.lingxi.identity.api.*;
import com.lingxi.kernel.ActorContextHolder;
import com.lingxi.kernel.ApiResponse;
import com.lingxi.kernel.RequestAttributes;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import org.springframework.web.bind.annotation.*;

/** 身份、会话与隐私权利的统一客户端接口。 */
@RestController
@RequestMapping("/api/v1")
public class IdentityController {
  private final RegistrationFacade registrationFacade;
  private final AuthenticationFacade authenticationFacade;
  private final PhoneLoginFacade phoneLoginFacade;
  private final PrivacyFacade privacyFacade;

  public IdentityController(
      RegistrationFacade registrationFacade,
      AuthenticationFacade authenticationFacade,
      PhoneLoginFacade phoneLoginFacade,
      PrivacyFacade privacyFacade) {
    this.registrationFacade = registrationFacade;
    this.authenticationFacade = authenticationFacade;
    this.phoneLoginFacade = phoneLoginFacade;
    this.privacyFacade = privacyFacade;
  }

  @PostMapping("/auth/registration-sessions")
  public ApiResponse<RegisteredUserResult> register(
      @RequestBody RegisterRequest body, HttpServletRequest request) {
    return ok(
        registrationFacade.register(
            new RegisterWithAssertionCommand(
                body.requestKey(), body.signedAssertion(), body.timezone())),
        request);
  }

  /**
   * 手机号登录；手机号尚未绑定账号时按出生日期自动建立账号。
   *
   * <p>当前不校验短信验证码，只应作为过渡入口使用；可用 `lingxi.identity.phone-login-enabled=false` 关闭。
   */
  @PostMapping("/auth/phone-sessions")
  public ApiResponse<SessionTokens> phoneLogin(
      @RequestBody PhoneLoginBody body, HttpServletRequest request) {
    return ok(
        phoneLoginFacade.loginWithPhone(
            new PhoneLoginCommand(
                body.phoneNumber(), body.birthDate(), body.deviceId(), body.timezone())),
        request);
  }

  @PostMapping("/auth/login")
  public ApiResponse<SessionTokens> login(
      @RequestBody LoginRequest body, HttpServletRequest request) {
    return ok(
        authenticationFacade.login(
            new LoginWithAssertionCommand(
                body.requestKey(), body.signedAssertion(), body.deviceId())),
        request);
  }

  @PostMapping("/auth/refresh")
  public ApiResponse<SessionTokens> refresh(
      @RequestBody RefreshRequest body, HttpServletRequest request) {
    return ok(
        authenticationFacade.refresh(
            new RefreshSessionCommand(
                body.sessionFamilyId(), body.refreshToken(), body.deviceId())),
        request);
  }

  @PostMapping("/privacy/requests")
  public ApiResponse<PrivacyRequestResult> createPrivacy(
      @RequestHeader("Idempotency-Key") String key,
      @RequestBody PrivacyRequestBody body,
      HttpServletRequest request) {
    long userId = ActorContextHolder.requireUser().actorId();
    return ok(
        privacyFacade.createRequest(
            new CreatePrivacyRequestCommand(
                key,
                userId,
                body.type(),
                body.scopeJson(),
                ActorContextHolder.requireUser().recentAuthentication())),
        request);
  }

  @GetMapping("/privacy/requests/{id}")
  public ApiResponse<PrivacyRequestResult> privacy(
      @PathVariable long id, HttpServletRequest request) {
    return ok(privacyFacade.getRequest(ActorContextHolder.requireUser().actorId(), id), request);
  }

  @PostMapping("/privacy/requests/{id}/cancel")
  public ApiResponse<PrivacyRequestResult> cancelPrivacy(
      @PathVariable long id, HttpServletRequest request) {
    return ok(
        privacyFacade.cancelAccountClosure(
            ActorContextHolder.requireUser().actorId(), id),
        request);
  }

  @GetMapping("/privacy/exports/{id}")
  public ApiResponse<PrivacyExportResult> download(
      @PathVariable long id, HttpServletRequest request) {
    return ok(
        privacyFacade.downloadExport(ActorContextHolder.requireUser().actorId(), id), request);
  }

  private <T> ApiResponse<T> ok(T value, HttpServletRequest request) {
    return ApiResponse.success(
        value, String.valueOf(request.getAttribute(RequestAttributes.REQUEST_ID)));
  }

  public record RegisterRequest(String requestKey, String signedAssertion, String timezone) {}

  /** 手机号登录请求；首次登录需要提供出生日期。 */
  public record PhoneLoginBody(
      String phoneNumber, LocalDate birthDate, String deviceId, String timezone) {}

  public record LoginRequest(String requestKey, String signedAssertion, String deviceId) {}

  public record RefreshRequest(String sessionFamilyId, String refreshToken, String deviceId) {}

  public record PrivacyRequestBody(PrivacyRequestType type, String scopeJson) {}
}
