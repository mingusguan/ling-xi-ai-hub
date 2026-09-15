package com.lingxi.goal.api;

import com.lingxi.kernel.PageResult;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/** 目标模块对其他模块公开的应用门面。 */
public interface GoalFacade {
  /** 创建草稿目标。 */
  GoalResult createGoal(CreateGoalCommand command);

  /** 更新目标定义；由乐观版本号保护，已放弃或已归档的目标不允许修改。 */
  GoalResult updateDefinition(UpdateGoalDefinitionCommand command);

  /** 目标生命周期流转：暂停、恢复、放弃、归档。 */
  GoalResult transition(TransitionGoalCommand command);

  /** 查询当前活跃目标配额，供客户端提示是否还能新增目标。 */
  GoalQuotaResult quota(long userId);

  /** 保存待用户确认的计划草案，不改变当前生效计划。 */
  PlanDraftResult savePlanDraft(SavePlanDraftCommand command);

  /** 原子激活既有计划草案。 */
  GoalResult confirmPlanDraft(ConfirmPlanDraftCommand command);

  /** 原子确认用户直接提交的计划版本、里程碑和行动。 */
  GoalResult confirmPlan(ConfirmPlanCommand command);

  /** 查询用户自己的目标。 */
  GoalResult getGoal(long userId, long goalId);

  /**
   * 查询入门目标模板目录（PRD ONB-02 的模板入口）。
   *
   * <p>模板对所有用户一致，因此只校验账号可访问核心功能，不按用户过滤内容。
   */
  List<StarterGoalTemplate> listStarterTemplates(long userId);

  /** 按业务键查询单个入门模板；不存在时抛 {@code GOAL_TEMPLATE_NOT_FOUND}。 */
  StarterGoalTemplate getStarterTemplate(long userId, String templateKey);

  /**
   * 推进首目标引导的澄清阶段。
   *
   * <p>PRD 要求「AI 每次最多提出一个关键澄清问题」且「用户可随时切换为手动创建」，
   * 因此阶段由客户端按用户回答逐步推进；本方法只记录进度，不改动目标状态机。
   */
  GoalResult advanceClarification(AdvanceClarificationCommand command);

  /** 查询用户全部目标，供 PC Web 与 HarmonyOS 的多目标列表使用。 */
  List<GoalResult> listGoals(long userId);

  /** 为行动按本地时间语义生成发生实例。 */
  int generateOccurrences(long actionId, LocalDate fromDate, LocalDate toDate);

  /** 查询用户行动实例。 */
  List<OccurrenceResult> listOccurrences(long userId, LocalDate fromDate, LocalDate toDate);

  /** 查询目标当前生效计划下的全部行动，供行动编辑界面使用。 */
  List<ActionResult> listActions(long userId, long goalId);

  /** 计划外新增行动；行动追加到目标当前生效的计划版本上。 */
  ActionResult addAction(AddActionCommand command);

  /** 编辑行动定义，「本次及未来」整体生效。 */
  ActionResult editAction(EditActionCommand command);

  /** 取消行动；重复调用是安全的空操作。 */
  ActionResult cancelAction(CancelActionCommand command);

  /** 复制行动为同计划内的新行动。 */
  ActionResult copyAction(CopyActionCommand command);

  /** 平移行动开始日期，「本次及未来」整体生效。 */
  ActionResult moveAction(MoveActionCommand command);

  /** 单次跳过或改期，不改写行动定义。 */
  OccurrenceResult adjustOccurrence(AdjustOccurrenceCommand command);

  /** 查询目标下已登记的单次调整例外。 */
  List<ActionExceptionResult> listActionExceptions(long userId, long goalId);

  // ---- 今日工作台 ----

  /**
   * 今日工作台聚合。
   *
   * @param timezone 调用方时区；「今天」按该时区判定，而不是服务端时区
   * @param upcomingDays 未来可见天数，不含今天
   */
  TodayResult today(long userId, String timezone, int upcomingDays);

  /** 开始专注会话；同一用户已有进行中会话时拒绝新建。 */
  FocusSessionResult startFocus(StartFocusCommand command);

  /** 暂停专注计时。 */
  FocusSessionResult pauseFocus(long userId, long sessionId, long expectedVersion);

  /** 恢复专注计时。 */
  FocusSessionResult resumeFocus(long userId, long sessionId, long expectedVersion);

  /** 结束专注并保留为一次记录，返回本次实际专注秒数。 */
  FocusSessionResult finishFocus(long userId, long sessionId, long expectedVersion, String note);

  /** 作废专注会话，本次时长不计入统计。 */
  FocusSessionResult abandonFocus(long userId, long sessionId, long expectedVersion);

  /** 查询当前进行中的专注会话；没有则为空。 */
  Optional<FocusSessionResult> activeFocus(long userId);

  /** 新建快速记录。 */
  QuickNoteResult createQuickNote(CreateQuickNoteCommand command);

  /** 查询最近的快速记录。 */
  List<QuickNoteResult> listQuickNotes(long userId, int limit);

  /** 删除快速记录。 */
  void deleteQuickNote(long userId, long noteId);

  /** 查询用户自己的行动实例，供提醒投递前校验实例是否仍可执行。 */
  Optional<OccurrenceResult> findOccurrence(long userId, long occurrenceId);

  /** 分页查询用户复盘，可按目标过滤，页码从 1 开始。 */
  com.lingxi.kernel.PageResult<ReviewResult> listReviews(
      long userId, Long goalId, int page, int pageSize);

  /** 查询用户自己的复盘明细。 */
  ReviewResult getReview(long userId, long reviewId);

  /** 幂等打卡或显式修正已有打卡。 */
  CheckInResult checkIn(CheckInCommand command);

  /**
   * 查询行动实例当前有效的打卡记录，供「更正」入口预填。
   *
   * <p>没有有效记录时返回空，不抛异常：未打卡是正常状态，不是错误。
   */
  Optional<CheckInView> findEffectiveCheckIn(long userId, long occurrenceId);

  /** 完成周期复盘。 */
  ReviewResult completeReview(CompleteReviewCommand command);
}
