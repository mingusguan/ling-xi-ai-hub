package com.lingxi.relationship.interfaces;

import com.lingxi.kernel.*;
import com.lingxi.relationship.api.*;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.Set;
import org.springframework.web.bind.annotation.*;

/** PC Web 与 HarmonyOS 共用的伙伴及分享接口。 */
@RestController
@RequestMapping("/api/v1")
public class PartnerController {
  private final PartnerFacade facade;

  public PartnerController(PartnerFacade facade) {
    this.facade = facade;
  }

  @PostMapping("/relationships/partners")
  public ApiResponse<PartnerRelationResult> invite(
      @RequestHeader("Idempotency-Key") String key,
      @RequestBody InviteBody b,
      HttpServletRequest r) {
    return ok(facade.invite(new InvitePartnerCommand(key, user(), b.inviteeUserId())), r);
  }

  @PostMapping("/relationships/partners/{id}/accept")
  public ApiResponse<PartnerRelationResult> accept(
      @PathVariable long id, @RequestBody VersionBody b, HttpServletRequest r) {
    return ok(facade.accept(new AcceptPartnerCommand(id, user(), b.expectedVersion())), r);
  }

  @DeleteMapping("/relationships/partners/{id}")
  public ApiResponse<PartnerRelationResult> terminate(
      @PathVariable long id,
      @RequestParam long expectedVersion,
      @RequestParam(defaultValue = "TERMINATE") String reason,
      HttpServletRequest r) {
    return ok(facade.terminate(user(), id, expectedVersion, reason), r);
  }

  @PutMapping("/relationships/partners/{id}/grants")
  public ApiResponse<PartnerGrantResult> grant(
      @PathVariable long id, @RequestBody GrantBody b, HttpServletRequest r) {
    return ok(
        facade.updateGrant(
            new UpdatePartnerGrantCommand(
                user(), id, b.goalId(), b.permissions(), b.expiresAt(), b.expectedVersion())),
        r);
  }

  @DeleteMapping("/relationships/grants/{id}")
  public ApiResponse<PartnerGrantResult> revokeGrant(
      @PathVariable long id, @RequestParam long expectedVersion, HttpServletRequest r) {
    return ok(facade.revokeGrant(user(), id, expectedVersion), r);
  }

  @PostMapping("/relationships/partners/interactions")
  public ApiResponse<PartnerInteractionResult> interact(
      @RequestBody InteractionBody b, HttpServletRequest r) {
    return ok(
        facade.interact(
            new CreatePartnerInteractionCommand(
                user(), b.goalId(), b.interactionType(), b.resourceId(), b.contentJson())),
        r);
  }

  @PostMapping("/reports")
  public ApiResponse<ReportResult> report(@RequestBody ReportBody b, HttpServletRequest r) {
    return ok(
        facade.report(
            new CreateReportCommand(
                user(), b.targetType(), b.targetId(), b.reasonCode(), b.evidenceReference())),
        r);
  }

  @PostMapping("/shares")
  public ApiResponse<ShareLinkResult> share(
      @RequestHeader("Idempotency-Key") String key,
      @RequestBody ShareBody b,
      HttpServletRequest r) {
    return ok(
        facade.createShare(
            new CreateShareCommand(
                key,
                user(),
                b.resourceType(),
                b.resourceId(),
                b.fields(),
                b.expiresAt(),
                b.visitLimit(),
                b.password())),
        r);
  }

  @PostMapping("/shares/access")
  public ApiResponse<ShareLinkResult> access(@RequestBody AccessBody b, HttpServletRequest r) {
    return ok(facade.accessShare(b.token(), b.password()), r);
  }

  @DeleteMapping("/shares/{id}")
  public ApiResponse<Void> revoke(
      @PathVariable long id, @RequestParam long expectedVersion, HttpServletRequest r) {
    facade.revokeShare(user(), id, expectedVersion);
    return ok(null, r);
  }

  private long user() {
    return ActorContextHolder.requireUser().actorId();
  }

  private <T> ApiResponse<T> ok(T v, HttpServletRequest r) {
    return ApiResponse.success(v, String.valueOf(r.getAttribute(RequestAttributes.REQUEST_ID)));
  }

  public record InteractionBody(
      long goalId, String interactionType, String resourceId, String contentJson) {}

  public record ReportBody(
      String targetType, String targetId, String reasonCode, String evidenceReference) {}

  public record InviteBody(long inviteeUserId) {}

  public record VersionBody(long expectedVersion) {}

  public record GrantBody(
      long goalId, Set<PartnerPermission> permissions, Instant expiresAt, long expectedVersion) {}

  public record ShareBody(
      String resourceType,
      String resourceId,
      Set<String> fields,
      Instant expiresAt,
      Integer visitLimit,
      String password) {}

  public record AccessBody(String token, String password) {}
}
