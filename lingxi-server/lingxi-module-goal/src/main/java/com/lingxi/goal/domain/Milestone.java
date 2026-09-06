package com.lingxi.goal.domain;

import com.lingxi.kernel.BusinessException;
import java.time.LocalDateTime;

/** 不可变计划版本中的里程碑。 */
public record Milestone(
    long id,
    long planVersionId,
    int sequenceNo,
    String title,
    String successCriteria,
    LocalDateTime createdAt) {
  public Milestone {
    if (id <= 0
        || planVersionId <= 0
        || sequenceNo <= 0
        || title == null
        || title.isBlank()
        || successCriteria == null
        || successCriteria.isBlank()) {
      throw new BusinessException("GOAL_INVALID_MILESTONE", "里程碑不合法");
    }
  }
}
