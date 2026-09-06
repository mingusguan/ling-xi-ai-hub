package com.lingxi.relationship.infrastructure.persistence;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/** 监护关系持久化对象。 */
@Getter
@Setter
@TableName("rel_guardian_relation")
public class GuardianRelationEntity extends RelationshipLogicalDeletionEntity {
  @TableId private Long id;
  private String requestKey;
  private String requestDigest;
  private Long teenUserId;
  private Long guardianUserId;
  private String invitationTokenHash;
  private String requestedPermissionsJson;
  private String status;
  private LocalDateTime expiresAt;
  private LocalDateTime verifiedAt;
  private LocalDateTime revokedAt;
  private String revokeReason;
  @Version private Long version;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
}
