package com.lingxi.identity.interfaces;

import com.lingxi.identity.api.AdminAuthorizationFacade;
import com.lingxi.identity.api.AdminAuthorizationFacade.AdminLoginCommand;
import com.lingxi.kernel.ActorContextHolder;
import com.lingxi.kernel.ApiResponse;
import com.lingxi.kernel.RequestAttributes;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

/** 管理端登录、当前身份与退出接口。 */
@RestController
@RequestMapping("/admin-api/v1/auth")
public class AdminIdentityController {
  private final AdminAuthorizationFacade facade;

  public AdminIdentityController(AdminAuthorizationFacade facade) {
    this.facade = facade;
  }

  @PostMapping("/login")
  public ApiResponse<AdminAuthorizationFacade.AdminSessionTokens> login(
      @RequestBody LoginBody body, HttpServletRequest request) {
    String forwarded = request.getHeader("X-Forwarded-For");
    String ip = forwarded == null ? request.getRemoteAddr() : forwarded.split(",")[0].trim();
    return ok(facade.login(new AdminLoginCommand(body.username(), body.password(),
        body.deviceId(), ip, request.getHeader("User-Agent"))), request);
  }

  @GetMapping("/profile")
  public ApiResponse<AdminAuthorizationFacade.AdminProfile> profile(HttpServletRequest request) {
    return ok(facade.profile(ActorContextHolder.requireAdmin().actorId()), request);
  }

  @PostMapping("/reauthenticate")
  public ApiResponse<Void> reauthenticate(
      @RequestHeader("Authorization") String authorization,
      @RequestBody ReauthenticateBody body,
      HttpServletRequest request) {
    long adminId = ActorContextHolder.requireAdmin().actorId();
    facade.reauthenticate(adminId, bearerToken(authorization), body.password());
    return ok(null, request);
  }

  @DeleteMapping("/logout")
  public ApiResponse<Void> logout(
      @RequestHeader("Authorization") String authorization, HttpServletRequest request) {
    long adminId = ActorContextHolder.requireAdmin().actorId();
    facade.logout(adminId, bearerToken(authorization));
    return ok(null, request);
  }

  private <T> ApiResponse<T> ok(T value, HttpServletRequest request) {
    return ApiResponse.success(value,
        String.valueOf(request.getAttribute(RequestAttributes.REQUEST_ID)));
  }

  public record LoginBody(String username, String password, String deviceId) {}
  public record ReauthenticateBody(String password) {}

  private String bearerToken(String authorization) {
    return authorization != null && authorization.startsWith("Bearer ")
        ? authorization.substring(7).trim() : "";
  }
}
