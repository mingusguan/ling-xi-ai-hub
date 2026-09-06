package com.lingxi.engagement.infrastructure.persistence;

import com.baomidou.mybatisplus.annotation.*;
import java.time.*;
import lombok.Getter;
import lombok.Setter;

/** R05 持久化对象。 */
@Getter
@Setter
@TableName("eng_sync_cursor")
public class SyncChangeEntity extends EngagementLogicalDeletionEntity {
  @TableId private Long id;
  private Long userId;
  private Long sequenceNo;
  private String domainName;
  private String resourceType;
  private String resourceId;
  private Long resourceVersion;
  private String operationType;
  private String snapshotJson;
  private LocalDateTime occurredAt;
}
