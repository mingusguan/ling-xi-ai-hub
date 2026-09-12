package com.lingxi.goal.application;

import com.lingxi.goal.api.CheckInResultType;
import com.lingxi.goal.api.GoalStatus;
import com.lingxi.goal.domain.Action;
import com.lingxi.goal.domain.ActionOccurrence;
import com.lingxi.goal.domain.Goal;
import java.util.Objects;

/**
 * 打卡后成就判定所需的领域快照。
 *
 * @param goal 打卡写入后的目标聚合
 * @param statusBefore 打卡前的目标状态，用于识别本次打卡是否使目标完成
 * @param action 本次打卡所属行动
 * @param occurrence 本次打卡的行动实例
 * @param checkInResult 本次打卡结果
 */
public record CheckInAchievementContext(
    Goal goal,
    GoalStatus statusBefore,
    Action action,
    ActionOccurrence occurrence,
    CheckInResultType checkInResult) {

  public CheckInAchievementContext {
    Objects.requireNonNull(goal, "目标不能为空");
    Objects.requireNonNull(statusBefore, "打卡前目标状态不能为空");
    Objects.requireNonNull(action, "行动不能为空");
    Objects.requireNonNull(occurrence, "行动实例不能为空");
    Objects.requireNonNull(checkInResult, "打卡结果不能为空");
  }
}
