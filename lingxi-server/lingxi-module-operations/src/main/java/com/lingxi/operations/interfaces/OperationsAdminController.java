package com.lingxi.operations.interfaces;

import com.lingxi.kernel.*;
import com.lingxi.operations.api.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin-api/v1")
public class OperationsAdminController {
  private final OperationsFacade facade;

  public OperationsAdminController(OperationsFacade f) {
    facade = f;
  }

  @PostMapping("/config-releases")
  public ApiResponse<ConfigReleaseResult> create(
      @RequestHeader("X-Operation-Reason") String reason,
      @RequestHeader("X-Ticket-No") String ticket,
      @RequestBody ReleaseBody b,
      HttpServletRequest r) {
    long admin = admin();
    return ok(
        facade.createRelease(
            new CreateConfigReleaseCommand(
                b.releaseKey(),
                b.configType(),
                b.versionNo(),
                b.contentRef(),
                b.contentDigest(),
                b.grayRule(),
                admin,
                audit(reason, ticket, r))),
        r);
  }

  @PostMapping("/config-releases/{id}/validate")
  public ApiResponse<ConfigReleaseResult> validate(
      @PathVariable long id,
      @RequestBody VersionBody b,
      @RequestHeader("X-Operation-Reason") String reason,
      @RequestHeader("X-Ticket-No") String ticket,
      HttpServletRequest r) {
    return action(id, b.expectedVersion(), reason, ticket, r, "VALIDATE");
  }

  @PostMapping("/config-releases/{id}/approve")
  public ApiResponse<ConfigReleaseResult> approve(
      @PathVariable long id,
      @RequestBody VersionBody b,
      @RequestHeader("X-Operation-Reason") String reason,
      @RequestHeader("X-Ticket-No") String ticket,
      HttpServletRequest r) {
    return action(id, b.expectedVersion(), reason, ticket, r, "APPROVE");
  }

  @PostMapping("/config-releases/{id}/gray")
  public ApiResponse<ConfigReleaseResult> gray(
      @PathVariable long id,
      @RequestBody VersionBody b,
      @RequestHeader("X-Operation-Reason") String reason,
      @RequestHeader("X-Ticket-No") String ticket,
      HttpServletRequest r) {
    return action(id, b.expectedVersion(), reason, ticket, r, "GRAY");
  }

  @PostMapping("/config-releases/{id}/publish")
  public ApiResponse<ConfigReleaseResult> publish(
      @PathVariable long id,
      @RequestBody VersionBody b,
      @RequestHeader("X-Operation-Reason") String reason,
      @RequestHeader("X-Ticket-No") String ticket,
      HttpServletRequest r) {
    return action(id, b.expectedVersion(), reason, ticket, r, "PUBLISH");
  }

  @PostMapping("/config-releases/{id}/rollback")
  public ApiResponse<ConfigReleaseResult> rollback(
      @PathVariable long id,
      @RequestBody VersionBody b,
      @RequestHeader("X-Operation-Reason") String reason,
      @RequestHeader("X-Ticket-No") String ticket,
      HttpServletRequest r) {
    return action(id, b.expectedVersion(), reason, ticket, r, "ROLLBACK");
  }

  @PutMapping("/support-tickets/{id}")
  public ApiResponse<SupportTicketResult> ticket(
      @PathVariable long id,
      @RequestBody TicketBody b,
      @RequestHeader("X-Operation-Reason") String reason,
      @RequestHeader("X-Ticket-No") String ticket,
      HttpServletRequest r) {
    long a = admin();
    return ok(
        facade.transitionTicket(
            new TransitionTicketCommand(
                a,
                id,
                b.status(),
                b.assigneeAdminId(),
                b.expectedVersion(),
                audit(reason, ticket, r))),
        r);
  }

  private ApiResponse<ConfigReleaseResult> action(
      long id, long version, String reason, String ticket, HttpServletRequest r, String op) {
    AdminActionCommand c = new AdminActionCommand(admin(), id, version, audit(reason, ticket, r));
    ConfigReleaseResult x =
        switch (op) {
          case "VALIDATE" -> facade.validateRelease(c);
          case "APPROVE" -> facade.approveRelease(c);
          case "GRAY" -> facade.startGray(c);
          case "PUBLISH" -> facade.publishRelease(c);
          default -> facade.rollbackRelease(c);
        };
    return ok(x, r);
  }

  private long admin() {
    return ActorContextHolder.requireAdmin().actorId();
  }

  private AuditContext audit(String reason, String ticket, HttpServletRequest r) {
    return new AuditContext(
        reason,
        ticket,
        String.valueOf(r.getAttribute(RequestAttributes.REQUEST_ID)),
        ActorContextHolder.requireAdmin().recentAuthentication());
  }

  private <T> ApiResponse<T> ok(T v, HttpServletRequest r) {
    return ApiResponse.success(v, String.valueOf(r.getAttribute(RequestAttributes.REQUEST_ID)));
  }

  public record ReleaseBody(
      String releaseKey,
      String configType,
      int versionNo,
      String contentRef,
      String contentDigest,
      String grayRule) {}

  public record VersionBody(long expectedVersion) {}

  public record TicketBody(String status, Long assigneeAdminId, long expectedVersion) {}

}
