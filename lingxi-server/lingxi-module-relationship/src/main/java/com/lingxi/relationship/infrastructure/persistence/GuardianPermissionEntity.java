package com.lingxi.relationship.infrastructure.persistence;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/** 监护权限批量持久化对象。 */
@Getter
@Setter
@TableName("rel_guardian_permission")
public class GuardianPermissionEntity extends RelationshipLogicalDeletionEntity {
  @TableId private Long id;
  private Long relationId;
  private String permission;
  private String scope;
  private LocalDateTime effectiveAt;
}
