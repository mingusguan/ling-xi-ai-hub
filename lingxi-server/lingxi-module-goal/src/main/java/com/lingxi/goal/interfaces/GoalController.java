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
            new CreateGoalCommand(
                key,
                userId(),
                new GoalDefinitionInput(
                    body.title(),
                    body.description(),
                    body.successCriteria(),
                    body.goalType(),
                    body.startDate(),
                    body.targetEndDate(),
                    body.priority(),
                    body.weeklyAvailableMinutes(),
                    body.resourceConstraints(),
                    body.verifiableOutcomes(),
                    body.privacyLevel()))),
        request);
  }

  /**
   * 更新目标定义。
   *
   * <p>本接口不要求 {@code Idempotency-Key}：写入由目标乐观版本号保护，
   * 同一份定义重复提交结果相同，重放不会产生额外副作用。
   */
  @PutMapping("/goals/{id}")
  public ApiResponse<GoalResult> updateDefinition(
      @PathVariable long id, @RequestBody UpdateGoalBody body, HttpServletRequest request) {
    return ok(
        goalFacade.updateDefinition(
            new UpdateGoalDefinitionCommand(
                "put-goal-" + id + "-" + body.expectedVersion(),
                userId(),
                id,
                body.expectedVersion(),
                new GoalDefinitionInput(
                    body.title(),
                    body.description(),
                    body.successCriteria(),
                    body.goalType(),
                    body.startDate(),
                    body.targetEndDate(),
                    body.priority(),
                    body.weeklyAvailableMinutes(),
                    body.resourceConstraints(),
                    body.verifiableOutcomes(),
                    body.privacyLevel()))),
        request);
  }

  /**
   * 目标生命周期流转：暂停、恢复、放弃、归档。
   *
   * <p>按目标状态幂等：重复提交同一动作时若目标已处于目标状态，直接返回当前结果，
   * 因此同样不要求 {@code Idempotency-Key}。
   */
  @PostMapping("/goals/{id}/transitions")
  public ApiResponse<GoalResult> transition(
      @PathVariable long id, @RequestBody TransitionBody body, HttpServletRequest request) {
    return ok(
        goalFacade.transition(
            new TransitionGoalCommand(
                "transition-" + id + "-" + body.transition() + "-" + body.expectedVersion(),
                userId(),
                id,
                body.expectedVersion(),
                body.transition(),
                body.expectedResumeDate(),
                body.abandonReason())),
        request);
  }

  /** 查询当前活跃目标配额，供客户端在创建与激活计划前提示。 */
  @GetMapping("/goal-quota")
  public ApiResponse<GoalQuotaResult> quota(HttpServletRequest request) {
    return ok(goalFacade.quota(userId()), request);
  }

  @GetMapping("/goals")
  public ApiResponse<List<GoalResult>> list(HttpServletRequest request) {
    return ok(goalFacade.listGoals(userId()), request);
  }

  @GetMapping("/goals/{id}")
  public ApiResponse<GoalResult> get(@PathVariable long id, HttpServletRequest request) {
    return ok(goalFacade.getGoal(userId(), id), request);
  }

  /**
   * 入门目标模板目录（PRD ONB-02 的模板入口）。
   *
   * <p>模板随版本交付、对所有用户一致，因此没有分页与筛选参数：数量是个位数量级，
   * 一次返回客户端本地过滤更简单。
   */
  @GetMapping("/goal-starter-templates")
  public ApiResponse<List<StarterGoalTemplate>> starterTemplates(HttpServletRequest request) {
    return ok(goalFacade.listStarterTemplates(userId()), request);
  }

  /** 按业务键读取单个入门模板，供客户端在创建目标前预览首行动。 */
  @GetMapping("/goal-starter-templates/{templateKey}")
  public ApiResponse<StarterGoalTemplate> starterTemplate(
      @PathVariable String templateKey, HttpServletRequest request) {
    return ok(goalFacade.getStarterTemplate(userId(), templateKey), request);
  }

  /**
   * 推进首目标引导澄清阶段。
   *
   * <p>不要求 {@code Idempotency-Key}：写入由目标乐观版本号保护，
   * 重复推进到同一阶段是空操作，重放不会产生额外副作用。
   */
  @PostMapping("/goals/{id}/clarification")
  public ApiResponse<GoalResult> advanceClarification(
      @PathVariable long id, @RequestBody ClarificationBody body, HttpServletRequest request) {
    return ok(
        goalFacade.advanceClarification(
            new AdvanceClarificationCommand(
                "clarification-" + id + "-" + body.stage() + "-" + body.expectedVersion(),
                userId(),
                id,
                body.stage(),
                body.expectedVersion())),
        request);
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

  /** 查询目标当前生效计划下的全部行动，供行动编辑界面使用。 */
  @GetMapping("/goals/{id}/actions")
  public ApiResponse<List<ActionResult>> actions(@PathVariable long id, HttpServletRequest request) {
    return ok(goalFacade.listActions(userId(), id), request);
  }

  /** 计划外新增行动；追加到目标当前生效的计划版本上。 */
  @PostMapping("/goals/{id}/actions")
  public ApiResponse<ActionResult> addAction(
      @PathVariable long id,
      @RequestHeader("Idempotency-Key") String key,
      @RequestBody AddActionBody body,
      HttpServletRequest request) {
    return ok(
        goalFacade.addAction(
            new AddActionCommand(key, userId(), id, body.milestoneSequence(), body.definition())),
        request);
  }

  /**
   * 编辑行动定义，「本次及未来」整体生效。
   *
   * <p>由行动乐观版本号保护，同一份定义重复提交结果相同，因此不要求 {@code Idempotency-Key}。
   */
  @PutMapping("/actions/{id}")
  public ApiResponse<ActionResult> editAction(
      @PathVariable long id, @RequestBody EditActionBody body, HttpServletRequest request) {
    return ok(
        goalFacade.editAction(
            new EditActionCommand(
                "edit-action-" + id + "-" + body.expectedVersion(),
                userId(),
                id,
                body.expectedVersion(),
                body.definition())),
        request);
  }

  /** 取消行动；已取消时重复调用是安全的空操作。 */
  @DeleteMapping("/actions/{id}")
  public ApiResponse<ActionResult> cancelAction(
      @PathVariable long id, @RequestParam long expectedVersion, HttpServletRequest request) {
    return ok(
        goalFacade.cancelAction(
            new CancelActionCommand("cancel-action-" + id, userId(), id, expectedVersion)),
        request);
  }

  /** 复制行动为同计划内的新行动。 */
  @PostMapping("/actions/{id}/copies")
  public ApiResponse<ActionResult> copyAction(
      @PathVariable long id,
      @RequestHeader("Idempotency-Key") String key,
      @RequestBody CopyActionBody body,
      HttpServletRequest request) {
    return ok(
        goalFacade.copyAction(
            new CopyActionCommand(key, userId(), id, body.clientKey(), body.title())),
        request);
  }

  /** 平移行动开始日期，「本次及未来」整体生效。 */
  @PostMapping("/actions/{id}/moves")
  public ApiResponse<ActionResult> moveAction(
      @PathVariable long id, @RequestBody MoveActionBody body, HttpServletRequest request) {
    return ok(
        goalFacade.moveAction(
            new MoveActionCommand(
                "move-action-" + id + "-" + body.newStartDate(),
                userId(),
                id,
                body.expectedVersion(),
                body.newStartDate())),
        request);
  }

  /**
   * 单次调整：只跳过或改期这一次，不改写行动的重复规则。
   *
   * <p>对应 PRD 的「单次修改」分支，与编辑行动的「本次及未来」严格分离。
   */
  @PostMapping("/action-occurrences/{id}/adjustments")
  public ApiResponse<OccurrenceResult> adjustOccurrence(
      @PathVariable long id,
      @RequestHeader("Idempotency-Key") String key,
      @RequestBody AdjustOccurrenceBody body,
      HttpServletRequest request) {
    return ok(
        goalFacade.adjustOccurrence(
            new AdjustOccurrenceCommand(
                key, userId(), id, body.type(), body.targetDate(), body.reason())),
        request);
  }

  /** 查询目标下已登记的单次调整例外。 */
  @GetMapping("/goals/{id}/action-exceptions")
  public ApiResponse<List<ActionExceptionResult>> actionExceptions(
      @PathVariable long id, HttpServletRequest request) {
    return ok(goalFacade.listActionExceptions(userId(), id), request);
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
                body.actualMinutes(),
                body.perceivedDifficulty(),
                body.energyLevel(),
                body.moodLevel(),
                body.failureReason(),
                body.correction())),
        request);
  }

  /**
   * 查询行动实例当前有效的打卡记录，供「更正」入口预填。
   *
   * <p>未打卡时 {@code data} 为 null：未打卡是正常状态，不是错误。
   */
  @GetMapping("/action-occurrences/{id}/check-in")
  public ApiResponse<CheckInView> effectiveCheckIn(
      @PathVariable long id, HttpServletRequest request) {
    return ok(goalFacade.findEffectiveCheckIn(userId(), id).orElse(null), request);
  }

  // ---- 今日工作台 ----

  /**
   * 今日工作台聚合：今天、逾期、接下来几天分组返回，并带上规则化提示与进行中的专注会话。
   *
   * <p>「今天」按 {@code timezone} 判定，不按服务端时区，否则东八区用户在早上 8 点前会拿到昨天的工作台。
   */
  @GetMapping("/today")
  public ApiResponse<TodayResult> today(
      @RequestParam(required = false) String timezone,
      @RequestParam(defaultValue = "7") int upcomingDays,
      HttpServletRequest request) {
    return ok(goalFacade.today(userId(), timezone, upcomingDays), request);
  }

  /** 开始专注会话；同一用户已有进行中会话时拒绝。 */
  @PostMapping("/focus-sessions")
  public ApiResponse<FocusSessionResult> startFocus(
      @RequestHeader("Idempotency-Key") String key,
      @RequestBody StartFocusBody body,
      HttpServletRequest request) {
    return ok(
        goalFacade.startFocus(
            new StartFocusCommand(key, userId(), body.occurrenceId(), body.plannedMinutes())),
        request);
  }

  /**
   * 查询当前进行中的专注会话。
   *
   * <p>没有进行中会话时 {@code data} 为 null：空闲是正常状态，不是错误。
   */
  @GetMapping("/focus-sessions/active")
  public ApiResponse<FocusSessionResult> activeFocus(HttpServletRequest request) {
    return ok(goalFacade.activeFocus(userId()).orElse(null), request);
  }

  /** 专注计时流转：暂停、恢复、结束、作废；由会话乐观版本号保护。 */
  @PostMapping("/focus-sessions/{id}/transitions")
  public ApiResponse<FocusSessionResult> transitionFocus(
      @PathVariable long id, @RequestBody FocusTransitionBody body, HttpServletRequest request) {
    long userId = userId();
    return switch (body.action()) {
      case PAUSE -> ok(goalFacade.pauseFocus(userId, id, body.expectedVersion()), request);
      case RESUME -> ok(goalFacade.resumeFocus(userId, id, body.expectedVersion()), request);
      case FINISH ->
          ok(goalFacade.finishFocus(userId, id, body.expectedVersion(), body.note()), request);
      case ABANDON -> ok(goalFacade.abandonFocus(userId, id, body.expectedVersion()), request);
    };
  }

  /** 新建快速记录：不绑定行动，随手写一句即可。 */
  @PostMapping("/quick-notes")
  public ApiResponse<QuickNoteResult> createQuickNote(
      @RequestHeader("Idempotency-Key") String key,
      @RequestBody QuickNoteBody body,
      HttpServletRequest request) {
    return ok(
        goalFacade.createQuickNote(
            new CreateQuickNoteCommand(key, userId(), body.content(), body.moodLevel(), body.timezone())),
        request);
  }

  /** 查询最近的快速记录，按创建时间倒序。 */
  @GetMapping("/quick-notes")
  public ApiResponse<List<QuickNoteResult>> quickNotes(
      @RequestParam(defaultValue = "20") int limit, HttpServletRequest request) {
    return ok(goalFacade.listQuickNotes(userId(), limit), request);
  }

  /** 删除快速记录；逻辑删除，历史数据不会被物理清除。 */
  @DeleteMapping("/quick-notes/{id}")
  public ApiResponse<Void> deleteQuickNote(@PathVariable long id, HttpServletRequest request) {
    goalFacade.deleteQuickNote(userId(), id);
    return ok(null, request);
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

  public record CreateGoalBody(
      String title,
      String description,
      String successCriteria,
      GoalType goalType,
      LocalDate startDate,
      LocalDate targetEndDate,
      PriorityLevel priority,
      Integer weeklyAvailableMinutes,
      String resourceConstraints,
      String verifiableOutcomes,
      GoalPrivacyLevel privacyLevel) {}

  public record UpdateGoalBody(
      long expectedVersion,
      String title,
      String description,
      String successCriteria,
      GoalType goalType,
      LocalDate startDate,
      LocalDate targetEndDate,
      PriorityLevel priority,
      Integer weeklyAvailableMinutes,
      String resourceConstraints,
      String verifiableOutcomes,
      GoalPrivacyLevel privacyLevel) {}

  public record TransitionBody(
      GoalTransition transition,
      long expectedVersion,
      LocalDate expectedResumeDate,
      String abandonReason) {}

  public record AddActionBody(Integer milestoneSequence, ActionInput definition) {}

  public record EditActionBody(long expectedVersion, ActionInput definition) {}

  public record CopyActionBody(String clientKey, String title) {}

  public record MoveActionBody(long expectedVersion, LocalDate newStartDate) {}

  public record AdjustOccurrenceBody(
      ActionExceptionType type, LocalDate targetDate, String reason) {}

  public record StartFocusBody(Long occurrenceId, Integer plannedMinutes) {}

  /** 专注会话流转动作。 */
  public enum FocusTransitionAction {
    PAUSE,
    RESUME,
    FINISH,
    ABANDON
  }

  public record FocusTransitionBody(
      FocusTransitionAction action, long expectedVersion, String note) {}

  public record QuickNoteBody(String content, MoodLevel moodLevel, String timezone) {}

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
      CheckInResultType result,
      String note,
      String evidenceReference,
      Integer actualMinutes,
      ActionDifficulty perceivedDifficulty,
      EnergyLevel energyLevel,
      MoodLevel moodLevel,
      String failureReason,
      boolean correction) {}

  public record ReviewBody(String conclusionJson) {}

  /** 推进首目标引导澄清阶段的请求体；{@code expectedVersion} 由目标乐观版本号保护。 */
  public record ClarificationBody(GoalClarificationStage stage, long expectedVersion) {}
}
