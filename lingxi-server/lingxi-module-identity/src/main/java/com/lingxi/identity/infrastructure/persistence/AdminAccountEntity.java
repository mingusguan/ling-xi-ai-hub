package com.lingxi.identity.infrastructure.persistence;

import com.baomidou.mybatisplus.annotation.*;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("id_admin_account")
public class AdminAccountEntity extends IdentityLogicalDeletionEntity {
  @TableId private Long id;
  private String username;
  private String displayName;
  private String passwordHash;
  private String status;
  private Integer failedAttempts;
  private LocalDateTime lockedUntil;
  private LocalDateTime passwordChangedAt;
  private LocalDateTime lastLoginAt;
  @Version private Long version;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
}
