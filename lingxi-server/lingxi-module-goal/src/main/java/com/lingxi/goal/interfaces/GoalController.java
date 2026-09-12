package com.lingxi.goal.interfaces;

import com.lingxi.goal.api.*;
import com.lingxi.kernel.ActorContextHolder;
import com.lingxi.kernel.ApiResponse;
import com.lingxi.kernel.PageResult;
import com.lingxi.kernel.RequestAttributes;
import jakarta.servlet.http.HttpServletRequest;
import java.time.*;
import java.util.List;
import org.springframework.web.bind.annotation.*;

/** PC Web 与 HarmonyOS 共用的目标执行接口。 */
@RestController
@RequestMapping("/api/v1")
public class GoalController {
  private final GoalFacade goalFacade;

  public GoalController(GoalFacade goalFacade) {
    this.goalFacade = goalFacade;
  }

  @PostMapping("/goals")
  public ApiResponse<GoalResult> create(
      @RequestHeader("Idempotency-Key") String key,
      @RequestBody CreateGoalBody body,
      HttpServletRequest request) {
    return ok(
        goalFacade.createGoal(
            new CreateGoalCommand(key, userId(), body.title(), body.successCriteria())),
        request);
  }

  @GetMapping("/goals")
  public ApiResponse<List<GoalResult>> list(HttpServletRequest request) {
    return ok(goalFacade.listGoals(userId()), request);
  }

  @GetMapping("/goals/{id}")
  public ApiResponse<GoalResult> get(@PathVariable long id, HttpServletRequest request) {
    return ok(goalFacade.getGoal(userId(), id), request);
  }

  @PostMapping("/goals/{id}/plan-drafts")
  public ApiResponse<PlanDraftResult> saveDraft(
      @PathVariable long id,
      @RequestHeader("Idempotency-Key") String key,
      @RequestBody PlanDraftBody body,
      HttpServletRequest request) {
    return ok(
        goalFacade.savePlanDraft(
            new SavePlanDraftCommand(
                key,
                userId(),
                id,
                body.planSnapshotJson(),
                body.adjustmentReason(),
                body.source(),
                body.milestones(),
                body.actions())),
        request);
  }

  @PostMapping("/plans/{id}/confirm")
  public ApiResponse<GoalResult> confirmDraft(
      @PathVariable long id,
      @RequestHeader("Idempotency-Key") String key,
      @RequestBody ConfirmDraftBody body,
      HttpServletRequest request) {
    return ok(
        goalFacade.confirmPlanDraft(
            new ConfirmPlanDraftCommand(key, userId(), id, body.expectedGoalVersion())),
        request);
  }

  @PostMapping("/goals/{id}/plan-confirmations")
  public ApiResponse<GoalResult> confirm(
      @PathVariable long id,
      @RequestHeader("Idempotency-Key") String key,
      @RequestBody ConfirmPlanBody body,
      HttpServletRequest request) {
    List<PlanMilestoneDraft> milestones = body.milestones() == null ? List.of() : body.milestones();
    List<PlanActionDraft> actions = body.actions() == null ? List.of() : body.actions();
    return ok(
        goalFacade.confirmPlan(
            new ConfirmPlanCommand(
                key,
                userId(),
                id,
                body.expectedGoalVersion(),
                body.planSnapshotJson(),
                body.adjustmentReason(),
                milestones,
                actions)),
        request);
  }

  @GetMapping("/action-occurrences")
  public ApiResponse<List<OccurrenceResult>> occurrences(
      @RequestParam LocalDate from, @RequestParam LocalDate to, HttpServletRequest request) {
    return ok(goalFacade.listOccurrences(userId(), from, to), request);
  }

  @PutMapping("/action-occurrences/{id}/check-ins")
  public ApiResponse<CheckInResult> checkIn(
      @PathVariable long id,
      @RequestHeader("Idempotency-Key") String key,
      @RequestBody CheckInBody body,
      HttpServletRequest request) {
    return ok(
        goalFacade.checkIn(
            new CheckInCommand(
                key,
                userId(),
                id,
                body.result(),
                body.note(),
                body.evidenceReference(),
                body.correction())),
        request);
  }

  @GetMapping("/reviews")
  public ApiResponse<PageResult<ReviewResult>> reviews(
      @RequestParam(required = false) Long goalId,
      @RequestParam(defaultValue = "1") int page,
      @RequestParam(defaultValue = "20") int pageSize,
      HttpServletRequest request) {
    return ok(goalFacade.listReviews(userId(), goalId, page, pageSize), request);
  }

  @GetMapping("/reviews/{id}")
  public ApiResponse<ReviewResult> review(@PathVariable long id, HttpServletRequest request) {
    return ok(goalFacade.getReview(userId(), id), request);
  }

  @PostMapping("/reviews/{id}/complete")
  public ApiResponse<ReviewResult> completeReview(
      @PathVariable long id,
      @RequestHeader("Idempotency-Key") String key,
      @RequestBody ReviewBody body,
      HttpServletRequest request) {
    return ok(
        goalFacade.completeReview(
            new CompleteReviewCommand(key, userId(), id, body.conclusionJson())),
        request);
  }

  private long userId() {
    return ActorContextHolder.requireUser().actorId();
  }

  private <T> ApiResponse<T> ok(T value, HttpServletRequest request) {
    return ApiResponse.success(
        value, String.valueOf(request.getAttribute(RequestAttributes.REQUEST_ID)));
  }

  public record CreateGoalBody(String title, String successCriteria) {}

  public record PlanDraftBody(
      String planSnapshotJson,
      String adjustmentReason,
      String source,
      List<PlanMilestoneDraft> milestones,
      List<PlanActionDraft> actions) {}

  public record ConfirmDraftBody(long expectedGoalVersion) {}

  public record ConfirmPlanBody(
      long expectedGoalVersion,
      String planSnapshotJson,
      String adjustmentReason,
      List<PlanMilestoneDraft> milestones,
      List<PlanActionDraft> actions) {}

  public record CheckInBody(
      CheckInResultType result, String note, String evidenceReference, boolean correction) {}

  public record ReviewBody(String conclusionJson) {}
}
