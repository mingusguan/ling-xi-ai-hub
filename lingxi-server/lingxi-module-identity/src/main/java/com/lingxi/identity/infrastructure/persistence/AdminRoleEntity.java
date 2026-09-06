package com.lingxi.identity.infrastructure.persistence;

import com.baomidou.mybatisplus.annotation.*;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter @TableName("id_admin_role")
public class AdminRoleEntity extends IdentityLogicalDeletionEntity {
  @TableId private Long id;
  private String roleKey;
  private String name;
  private String status;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
}
