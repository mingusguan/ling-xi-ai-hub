package com.lingxi.goal.infrastructure.persistence;

import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/** 行动单次调整例外持久化对象。 */
@Getter
@Setter
@TableName("goal_action_exception")
public class ActionExceptionEntity {
  private Long id;
  private Long actionId;
  private LocalDate localDate;
  private String exceptionType;
  private LocalDate rescheduledDate;
  private String reason;
  private LocalDateTime createdAt;
}
