package com.lingxi.engagement.infrastructure.persistence;

import com.baomidou.mybatisplus.annotation.*;
import java.time.*;
import lombok.Getter;
import lombok.Setter;

/** R05 持久化对象。 */
@Getter
@Setter
@TableName("eng_inbox_message")
public class InboxMessageEntity extends EngagementLogicalDeletionEntity {
  @TableId private Long id;
  private Long userId;
  private String type;
  private String resourceType;
  private String resourceId;
  private String summary;
  /**
   * 站内信游标列。
   *
   * <p>字段名与列名保持 `cursorNo`/`cursor_no` 一致：`cursor` 是 MySQL 保留字，若使用自定义
   * `@TableField("cursor_no")` 映射到名为 cursor 的属性，MyBatis-Plus 会生成 `cursor_no AS cursor`
   * 这种未加反引号的别名，导致 MySQL 语法错误。
   */
  private Long cursorNo;
  private LocalDateTime readAt;
  private LocalDateTime createdAt;
}
