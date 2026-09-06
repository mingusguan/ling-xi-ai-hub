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

  int insertReviews(List<Review> reviews);

  boolean updateReview(Review review, long previousVersion);

  int logicallyDeleteUserData(long userId);
}
