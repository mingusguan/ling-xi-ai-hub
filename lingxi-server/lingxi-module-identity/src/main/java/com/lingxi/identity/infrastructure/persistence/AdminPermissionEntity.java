package com.lingxi.identity.infrastructure.persistence;

import com.baomidou.mybatisplus.annotation.*;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter @TableName("id_admin_permission")
public class AdminPermissionEntity {
  @TableId private Long id;
  private String permissionKey;
  private String name;
  private String permissionGroup;
  private String riskLevel;
  private String status;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
}
