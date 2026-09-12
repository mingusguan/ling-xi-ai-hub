package com.lingxi.goal.infrastructure.privacy;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lingxi.goal.domain.AchievementRepository;
import com.lingxi.goal.domain.GoalRepository;
import com.lingxi.goal.infrastructure.persistence.*;
import com.lingxi.identity.api.*;
import java.util.*;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** 导出或清理目标、计划、行动、打卡、复盘与成就数据。 */
@Component
public class GoalPrivacyDataContributor implements PrivacyDataContributor {
  private final GoalRepository repository;
  private final AchievementRepository achievementRepository;
  private final GoalMapper goalMapper;
  private final PlanVersionMapper planMapper;
  private final MilestoneMapper milestoneMapper;
  private final ActionMapper actionMapper;
  private final OccurrenceMapper occurrenceMapper;
  private final CheckInMapper checkInMapper;
  private final ReviewMapper reviewMapper;
  private final AchievementMapper achievementMapper;
  private final ObjectMapper objectMapper;

  public GoalPrivacyDataContributor(
      GoalRepository repository,
      AchievementRepository achievementRepository,
      GoalMapper goalMapper,
      PlanVersionMapper planMapper,
      MilestoneMapper milestoneMapper,
      ActionMapper actionMapper,
      OccurrenceMapper occurrenceMapper,
      CheckInMapper checkInMapper,
      ReviewMapper reviewMapper,
      AchievementMapper achievementMapper,
      ObjectMapper objectMapper) {
    this.repository = repository;
    this.achievementRepository = achievementRepository;
    this.goalMapper = goalMapper;
    this.planMapper = planMapper;
    this.milestoneMapper = milestoneMapper;
    this.actionMapper = actionMapper;
    this.occurrenceMapper = occurrenceMapper;
    this.checkInMapper = checkInMapper;
    this.reviewMapper = reviewMapper;
    this.achievementMapper = achievementMapper;
    this.objectMapper = objectMapper;
  }

  @Override
  public String moduleName() {
    return "goal";
  }

  @Override
  @Transactional
  public PrivacyContribution process(PrivacyProcessingContext context) {
    long userId = context.userId();
    PrivacyRequestType type = context.type();
    if (type == PrivacyRequestType.DELETE_DATA || type == PrivacyRequestType.CLOSE_ACCOUNT) {
      // 成就同样属于用户数据，与目标执行数据一并逻辑删除。
      return PrivacyContribution.deleted(
          repository.logicallyDeleteUserData(userId)
              + achievementRepository.logicallyDeleteUserData(userId));
    }
    if (type != PrivacyRequestType.EXPORT) {
      return PrivacyContribution.unchanged();
    }
    List<GoalEntity> goals =
        goalMapper.selectList(Wrappers.<GoalEntity>lambdaQuery().eq(GoalEntity::getUserId, userId));
    List<Long> goalIds = goals.stream().map(GoalEntity::getId).toList();
    Map<String, Object> data = new LinkedHashMap<>();
    data.put("goals", goals);
    data.put(
        "achievements",
        achievementMapper.selectList(
            Wrappers.<AchievementEntity>lambdaQuery().eq(AchievementEntity::getUserId, userId)));
    if (goalIds.isEmpty()) {
      return PrivacyContribution.exported(json(data));
    }
    List<PlanVersionEntity> plans =
        planMapper.selectList(
            Wrappers.<PlanVersionEntity>lambdaQuery().in(PlanVersionEntity::getGoalId, goalIds));
    List<Long> planIds = plans.stream().map(PlanVersionEntity::getId).toList();
    List<ActionEntity> actions =
        actionMapper.selectList(
            Wrappers.<ActionEntity>lambdaQuery().in(ActionEntity::getGoalId, goalIds));
    List<Long> actionIds = actions.stream().map(ActionEntity::getId).toList();
    data.put("plans", plans);
    data.put("actions", actions);
    data.put(
        "milestones",
        planIds.isEmpty()
            ? List.of()
            : milestoneMapper.selectList(
                Wrappers.<MilestoneEntity>lambdaQuery()
                    .in(MilestoneEntity::getPlanVersionId, planIds)));
    List<OccurrenceEntity> occurrences =
        actionIds.isEmpty()
            ? List.of()
            : occurrenceMapper.selectList(
                Wrappers.<OccurrenceEntity>lambdaQuery()
                    .in(OccurrenceEntity::getActionId, actionIds));
    data.put("occurrences", occurrences);
    List<Long> occurrenceIds = occurrences.stream().map(OccurrenceEntity::getId).toList();
    data.put(
        "checkIns",
        occurrenceIds.isEmpty()
            ? List.of()
            : checkInMapper.selectList(
                Wrappers.<CheckInEntity>lambdaQuery()
                    .in(CheckInEntity::getOccurrenceId, occurrenceIds)));
    data.put(
        "reviews",
        reviewMapper.selectList(
            Wrappers.<ReviewEntity>lambdaQuery().in(ReviewEntity::getGoalId, goalIds)));
    return PrivacyContribution.exported(json(data));
  }

  private String json(Object value) {
    try {
      return objectMapper.writeValueAsString(value);
    } catch (JsonProcessingException e) {
      throw new IllegalStateException("目标数据导出失败", e);
    }
  }
}
