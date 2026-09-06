package com.lingxi.relationship.infrastructure.persistence;

import com.baomidou.mybatisplus.annotation.*;
import java.time.*;
import lombok.*;

@Getter
@Setter
@TableName("rel_partner_interaction")
public class PartnerInteractionEntity extends RelationshipLogicalDeletionEntity {
  @TableId private Long id;
  private Long relationId, grantId, actorUserId;
  private String interactionType, resourceId, contentJson;
  private LocalDateTime createdAt;
}
