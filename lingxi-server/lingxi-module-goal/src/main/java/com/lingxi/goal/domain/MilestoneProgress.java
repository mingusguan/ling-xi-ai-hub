package com.lingxi.goal.domain;

import java.util.List;
import java.util.Objects;

/**
 * 里程碑完成判定的输入快照。
 *
 * @param userId 里程碑归属用户
 * @param milestone 待判定的里程碑
 * @param actions 里程碑下未取消的行动
 * @param occurrences 上述行动已生成的全部实例
 */
public record MilestoneProgress(
    long userId,
    Milestone milestone,
    List<Action> actions,
    List<ActionOccurrence> occurrences) {

  public MilestoneProgress {
    if (userId <= 0) {
      throw new IllegalArgumentException("里程碑归属用户不合法");
    }
    Objects.requireNonNull(milestone, "里程碑不能为空");
    actions = actions == null ? List.of() : List.copyOf(actions);
    occurrences = occurrences == null ? List.of() : List.copyOf(occurrences);
  }
}
