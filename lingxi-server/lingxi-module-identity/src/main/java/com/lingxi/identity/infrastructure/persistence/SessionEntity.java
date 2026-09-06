package com.lingxi.identity.infrastructure.persistence;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/** Opaque Token 会话持久化对象。 */
@Getter
@Setter
@TableName("id_session")
public class SessionEntity extends IdentityLogicalDeletionEntity {
  @TableId private Long id;
  private String familyId;
  private Long userId;
  private String deviceId;
  private String accessTokenHash;
  private String refreshTokenHash;
  private String previousRefreshTokenHash;
  private Long authorizationVersion;
  private LocalDateTime accessExpiresAt;
  private LocalDateTime refreshExpiresAt;
  private LocalDateTime revokedAt;
  private String revokeReason;
  @Version private Long version;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
}
