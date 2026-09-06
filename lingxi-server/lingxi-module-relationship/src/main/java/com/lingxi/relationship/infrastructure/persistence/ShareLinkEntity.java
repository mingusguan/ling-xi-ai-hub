package com.lingxi.relationship.infrastructure.persistence;

import com.baomidou.mybatisplus.annotation.*;
import java.time.*;
import lombok.*;

@Getter
@Setter
@TableName("rel_share_link")
public class ShareLinkEntity extends RelationshipLogicalDeletionEntity {
  @TableId private Long id;
  private String requestKey, requestDigest;
  private Long ownerUserId;
  private String resourceType, resourceId, fieldsJson, snapshotJson, tokenHash, passwordHash;
  private Instant expiresAt;
  private Integer visitLimit, visitCount;
  private String status;
  @Version private Long version;
  private LocalDateTime createdAt, updatedAt;
}
