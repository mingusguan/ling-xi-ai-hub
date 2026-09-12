package com.lingxi.goal.domain;

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

  Optional<Action> findAction(long actionId);

  List<Action> findActionsByGoalIds(List<Long> goalIds);

  Optional<ActionOccurrence> findOccurrence(long occurrenceId);

  List<ActionOccurrence> findOccurrences(
      List<Long> actionIds, LocalDate fromDate, LocalDate toDate);

  int insertOccurrences(List<ActionOccurrence> occurrences);

  boolean updateOccurrence(ActionOccurrence occurrence, String expectedStatus);

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

  /** 查询里程碑下的行动，用于成就判定。 */
  List<Action> findActionsByMilestone(long milestoneId);

  /** 查询指定行动已生成的全部实例，用于成就判定。 */
  List<ActionOccurrence> findOccurrencesByActionIds(List<Long> actionIds);

  /** 查询用户当前有效行动在窗口内已完成打卡的本地日期，用于连续打卡判定。 */
  List<LocalDate> findCompletedCheckInDates(long userId, LocalDate fromDate, LocalDate toDate);

  int logicallyDeleteUserData(long userId);
}
