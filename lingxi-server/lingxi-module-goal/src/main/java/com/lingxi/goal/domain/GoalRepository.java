package com.lingxi.goal.domain;

import com.lingxi.goal.api.StarterGoalTemplate;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/** 目标聚合和执行闭环持久化端口。 */
public interface GoalRepository {
  Optional<Goal> findById(long goalId);

  Optional<Goal> findByRequestKey(String requestKey);

  List<Goal> findActiveGoals();

  List<Goal> findByUserId(long userId);

  Optional<PlanVersion> findPlanByRequestKey(String requestKey);

  Optional<PlanVersion> findPlanById(long planVersionId);

  int nextPlanVersionNo(long goalId);

  void insert(Goal goal);

  void insertPlanBundle(PlanVersion planVersion, List<Milestone> milestones, List<Action> actions);

  void supersedeOtherPlans(long goalId, long activePlanId);

  boolean activatePlanVersion(PlanVersion planVersion, PlanVersionStatus expectedStatus);

  void activatePlanActions(long planVersionId);

  boolean updateActivatedPlan(Goal goal, long previousVersion);

  /** 按乐观版本更新目标定义与生命周期字段（暂停、恢复、放弃、归档）。 */
  boolean updateDefinitionAndState(Goal goal, long previousVersion);

  /**
   * 统计用户已计入活跃上限的目标数。
   *
   * @param excludeGoalId 需要排除的目标标识，通常为当前正在激活的目标；无排除项时传 0
   */
  int countActiveGoals(long userId, long excludeGoalId);

  Optional<Action> findAction(long actionId);

  /** 追加单个行动，用于「计划外新增行动」。 */
  void insertAction(Action action);

  /** 按乐观版本更新行动定义；「本次及未来」修改走这里。 */
  boolean updateAction(Action action, long expectedVersion);

  List<Action> findActionsByGoalIds(List<Long> goalIds);

  /** 查询指定行动的单次调整例外。 */
  List<ActionException> findActionExceptions(List<Long> actionIds);

  /** 新增或覆盖单次调整例外；同一行动同一天只保留一条。 */
  void upsertActionException(ActionException exception);

  /** 撤销某一天的单次调整例外。 */
  boolean deleteActionException(long actionId, LocalDate localDate);

  Optional<ActionOccurrence> findOccurrence(long occurrenceId);

  List<ActionOccurrence> findOccurrences(
      List<Long> actionIds, LocalDate fromDate, LocalDate toDate);

  int insertOccurrences(List<ActionOccurrence> occurrences);

  boolean updateOccurrence(ActionOccurrence occurrence, String expectedStatus);

  /**
   * 删除指定行动在给定时刻之后仍未执行的实例。
   *
   * <p>修改行动时间或重复规则后必须清掉不再符合规则的未来实例，否则会出现「改了却没生效」；
   * 已打卡或已跳过的实例属于历史事实，一律保留。
   */
  int deleteFutureScheduledOccurrences(long actionId, java.time.LocalDateTime after);

  /**
   * 删除指定行动在某一时刻的未执行实例。
   *
   * <p>单次调整必须按「行动 + 时刻」精确定位：用范围比较需要构造开区间边界，
   * 而 DATETIME(3) 会把超过毫秒精度的边界值四舍五入，边界恰好落到实例时刻上时
   * 目标实例反而被排除在外。精确删除没有这个歧义。
   */
  boolean deleteScheduledOccurrenceAt(long actionId, java.time.LocalDateTime scheduledAt);

  Optional<CheckIn> findCheckInByRequest(long occurrenceId, String requestKey);

  Optional<CheckIn> findEffectiveCheckIn(long occurrenceId);

  void supersedeCheckIn(long checkInId);

  void insertCheckIn(CheckIn checkIn);

  int calculateProgress(long goalId, LocalDateTime asOf);

  boolean updateProgress(Goal goal, long previousVersion);

  Optional<Review> findReviewByGoalAndPeriod(long goalId, String periodKey);

  Optional<Review> findReview(long reviewId);

  /** 统计用户复盘总数，可按目标过滤。 */
  long countReviews(long userId, Long goalId);

  /** 分页查询用户复盘，按周期倒序，页码从 1 开始。 */
  List<Review> findReviewsByUser(long userId, Long goalId, int page, int pageSize);

  int insertReviews(List<Review> reviews);

  boolean updateReview(Review review, long previousVersion);

  /** 查询里程碑，用于成就判定。 */
  Optional<Milestone> findMilestone(long milestoneId);

  /** 按计划版本与序号定位里程碑，用于把计划外新增的行动挂到正确阶段下。 */
  Optional<Milestone> findMilestoneByPlanAndSequence(long planVersionId, int sequenceNo);

  /** 查询里程碑下的行动，用于成就判定。 */
  List<Action> findActionsByMilestone(long milestoneId);

  /** 查询指定行动已生成的全部实例，用于成就判定。 */
  List<ActionOccurrence> findOccurrencesByActionIds(List<Long> actionIds);

  /** 查询用户当前有效行动在窗口内已完成打卡的本地日期，用于连续打卡判定。 */
  List<LocalDate> findCompletedCheckInDates(long userId, LocalDate fromDate, LocalDate toDate);

  int logicallyDeleteUserData(long userId);

  // ---- 今日工作台：专注会话与快速记录 ----

  /** 查询用户当前进行中的专注会话；同一用户最多一个。 */
  Optional<FocusSession> findActiveFocusSession(long userId);

  Optional<FocusSession> findFocusSession(long sessionId);

  void insertFocusSession(FocusSession session);

  /** 按乐观版本更新专注会话；结束与作废会把进行中标记一并清空。 */
  boolean updateFocusSession(FocusSession session, long expectedVersion);

  /** 追加快速记录。 */
  void insertQuickNote(QuickNote note);

  /** 按创建时间倒序查询用户的快速记录，最多 {@code limit} 条。 */
  List<QuickNote> findQuickNotes(long userId, int limit);

  /** 查询用户最近结束的专注会话，用于今日专注时长汇总。 */
  List<FocusSession> findFocusSessionsSince(long userId, java.time.Instant since);

  /** 逻辑删除一条快速记录。 */
  boolean deleteQuickNote(long noteId);

  /**
   * 查询全部启用的入门目标模板，按展示顺序返回。
   *
   * <p>模板是随版本交付的产品自带数据、对所有用户一致，因此不按用户过滤；
   * 将来若需要按年龄分层限制模板范围，再在应用层按账号年龄段裁剪。
   */
  List<StarterGoalTemplate> findStarterTemplates();

  /** 按业务键查询单个入门模板；不存在时返回空。 */
  Optional<StarterGoalTemplate> findStarterTemplate(String templateKey);
}
