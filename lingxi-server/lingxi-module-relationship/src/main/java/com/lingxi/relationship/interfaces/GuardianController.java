package com.lingxi.relationship.interfaces;

import com.lingxi.kernel.ActorContextHolder;
import com.lingxi.kernel.ApiResponse;
import com.lingxi.kernel.RequestAttributes;
import com.lingxi.relationship.api.*;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Set;
import org.springframework.web.bind.annotation.*;

/** 监护邀请、接受与撤销接口。 */
@RestController
@RequestMapping("/api/v1/guardian-relations")
public class GuardianController {
  private final GuardianFacade guardianFacade;

  public GuardianController(GuardianFacade guardianFacade) {
    this.guardianFacade = guardianFacade;
  }

  @PostMapping("/invitations")
  public ApiResponse<GuardianInvitationResult> invite(
      @RequestHeader("Idempotency-Key") String key,
      @RequestBody InviteBody body,
      HttpServletRequest request) {
    return ok(
        guardianFacade.createInvitation(
            new CreateGuardianInvitationCommand(key, userId(), body.permissions())),
        request);
  }

  @PostMapping("/invitations/acceptance")
  public ApiResponse<GuardianRelationResult> accept(
      @RequestBody AcceptBody body, HttpServletRequest request) {
    return ok(
        guardianFacade.acceptInvitation(
            new AcceptGuardianInvitationCommand(body.invitationToken(), userId())),
        request);
  }

  @DeleteMapping("/{id}")
  public ApiResponse<GuardianRelationResult> revoke(
      @PathVariable long id, @RequestBody RevokeBody body, HttpServletRequest request) {
    return ok(
        guardianFacade.revokeRelation(
            new RevokeGuardianRelationCommand(id, userId(), body.reason())),
        request);
  }

  @GetMapping("/{id}")
  public ApiResponse<GuardianRelationResult> get(
      @PathVariable long id, HttpServletRequest request) {
    return ok(guardianFacade.getRelation(id, userId()), request);
  }

  private long userId() {
    return ActorContextHolder.requireUser().actorId();
  }

  private <T> ApiResponse<T> ok(T value, HttpServletRequest request) {
    return ApiResponse.success(
        value, String.valueOf(request.getAttribute(RequestAttributes.REQUEST_ID)));
  }

  public record InviteBody(Set<GuardianPermissionType> permissions) {}

  public record AcceptBody(String invitationToken) {}

  public record RevokeBody(String reason) {}
}
