package com.lingxi.goal.infrastructure.persistence;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/** 入门目标模板的持久化对象；数据由版本迁移种入，运行时只读。 */
@Getter
@Setter
@TableName("goal_starter_template")
public class StarterGoalTemplateEntity extends GoalLogicalDeletionEntity {
  @TableId private Long id;
  private String templateKey;
  private String name;
  private String summary;
  private String goalType;
  private String defaultSuccessCriteria;
  private String tagsJson;
  /** 首行动候选 JSON 数组。 */
  private String firstActionsJson;
  private Integer displayOrder;
  private Boolean enabled;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
}
