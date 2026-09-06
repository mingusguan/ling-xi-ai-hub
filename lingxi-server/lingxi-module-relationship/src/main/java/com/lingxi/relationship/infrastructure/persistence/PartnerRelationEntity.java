package com.lingxi.relationship.infrastructure.persistence;

import com.baomidou.mybatisplus.annotation.*;
import java.time.LocalDateTime;
import lombok.*;

@Getter
@Setter
@TableName("rel_partner_relation")
public class PartnerRelationEntity extends RelationshipLogicalDeletionEntity {
  @TableId private Long id;
  private String requestKey;
  private Long inviterUserId;
  private Long inviteeUserId;
  private String status;
  @Version private Long version;
  private LocalDateTime createdAt, updatedAt;
}
