package com.lingxi.content.interfaces;

import com.lingxi.content.api.*;
import com.lingxi.kernel.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin-api/v1/templates")
public class ContentAdminController {
  private final ContentFacade facade;
  private final TemplateGovernanceFacade governance;

  public ContentAdminController(ContentFacade f, TemplateGovernanceFacade governance) {
    facade = f;
    this.governance = governance;
  }

  @GetMapping
  public ApiResponse<java.util.List<TemplateVersionResult>> list(
      @RequestParam(required = false) String status, HttpServletRequest r) {
    return ok(facade.listAdminTemplates(user(), status), r);
  }

  @PostMapping
  public ApiResponse<TemplateVersionResult> create(
      @RequestBody CreateBody b, HttpServletRequest r) {
    return ok(
        facade.createTemplate(
            new CreateTemplateVersionCommand(
                b.templateKey(),
                b.name(),
                b.versionNo(),
                b.ageScope(),
                b.contentSnapshot(),
                user())),
        r);
  }

  @PostMapping("/{id}/submit")
  public ApiResponse<TemplateVersionResult> submit(
      @PathVariable long id, @RequestBody VersionBody b, HttpServletRequest r) {
    return ok(facade.submitTemplate(user(), id, b.expectedVersion()), r);
  }

  @PostMapping("/{id}/review")
  public ApiResponse<TemplateVersionResult> review(
      @PathVariable long id, @RequestBody ReviewBody b,
      @RequestHeader("X-Operation-Reason") String operationReason,
      @RequestHeader("X-Ticket-No") String ticketNo, HttpServletRequest r) {
    ActorContext actor = ActorContextHolder.requireAdmin();
    return ok(facade.reviewTemplate(new ContentFacade.TemplateReviewCommand(actor.actorId(), id,
        b.approved(), b.reason(), b.expectedVersion(), actor.recentAuthentication(),
        operationReason, ticketNo)), r);
  }

  @PostMapping("/{id}/retire")
  public ApiResponse<TemplateVersionResult> retire(
      @PathVariable long id, @RequestBody VersionBody b,
      @RequestHeader("X-Operation-Reason") String operationReason,
      @RequestHeader("X-Ticket-No") String ticketNo, HttpServletRequest r) {
    ActorContext actor = ActorContextHolder.requireAdmin();
    return ok(facade.retireTemplate(new ContentFacade.TemplateRetireCommand(actor.actorId(), id,
        b.expectedVersion(), actor.recentAuthentication(), operationReason, ticketNo)), r);
  }

  @GetMapping("/{id}/reviews")
  public ApiResponse<?> reviews(@PathVariable long id, HttpServletRequest r) {
    return ok(governance.reviews(user(), id), r);
  }

  @PostMapping("/{id}/reviews")
  public ApiResponse<?> reviewDimension(@PathVariable long id, @RequestBody DimensionReviewBody b,
      @RequestHeader("X-Operation-Reason") String operationReason,
      @RequestHeader("X-Ticket-No") String ticketNo, HttpServletRequest r) {
    ActorContext actor = ActorContextHolder.requireAdmin();
    return ok(governance.reviewDimension(new TemplateGovernanceFacade.ReviewCommand(
        actor.actorId(), id, b.dimension(), b.decision(), b.comment(), b.expectedVersion(),
        new TemplateGovernanceFacade.OperationContext(operationReason, ticketNo,
            String.valueOf(r.getAttribute(RequestAttributes.REQUEST_ID)),
            actor.recentAuthentication()))), r);
  }

  @GetMapping("/{id}/metrics")
  public ApiResponse<?> metrics(@PathVariable long id,
      @RequestParam(required = false) java.time.LocalDate from,
      @RequestParam(required = false) java.time.LocalDate to, HttpServletRequest r) {
    return ok(governance.metrics(user(), id, from, to), r);
  }

  private long user() {
    return ActorContextHolder.requireAdmin().actorId();
  }

  private <T> ApiResponse<T> ok(T v, HttpServletRequest r) {
    return ApiResponse.success(v, String.valueOf(r.getAttribute(RequestAttributes.REQUEST_ID)));
  }

  public record CreateBody(
      String templateKey, String name, int versionNo, String ageScope, String contentSnapshot) {}

  public record VersionBody(long expectedVersion) {}

  public record ReviewBody(boolean approved, String reason, long expectedVersion) {}
  public record DimensionReviewBody(String dimension, String decision, String comment,
      long expectedVersion) {}
}
