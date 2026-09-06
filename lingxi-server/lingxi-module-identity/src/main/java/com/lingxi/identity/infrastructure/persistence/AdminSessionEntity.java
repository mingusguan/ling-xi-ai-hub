package com.lingxi.identity.infrastructure.persistence;

import com.baomidou.mybatisplus.annotation.*;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("id_admin_session")
public class AdminSessionEntity extends IdentityLogicalDeletionEntity {
  @TableId private Long id;
  private Long adminId;
  private String accessTokenHash;
  private String deviceId;
  private String ipAddress;
  private String userAgent;
  private LocalDateTime authenticatedAt;
  private LocalDateTime expiresAt;
  private LocalDateTime revokedAt;
  private String revokeReason;
  private LocalDateTime createdAt;
}
