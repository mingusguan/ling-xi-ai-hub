package com.lingxi.identity.infrastructure.persistence;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/** 隐私逻辑删除模块执行审计。 */
@Getter
@Setter
@TableName("id_privacy_deletion_audit")
public class PrivacyDeletionAuditEntity {
  @TableId private Long id;
  private Long requestId;
  private Long userId;
  private String moduleName;
  private Integer affectedRows;
  private LocalDateTime completedAt;
}
