package com.lingxi.identity.infrastructure.persistence;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("id_admin_account_role")
public class AdminAccountRoleEntity extends IdentityLogicalDeletionEntity {
  @TableId private Long id;
  private Long adminId;
  private Long roleId;
  private LocalDateTime createdAt;
}
