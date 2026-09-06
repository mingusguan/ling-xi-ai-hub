package com.lingxi.engagement.infrastructure.persistence;

import com.baomidou.mybatisplus.annotation.*;
import java.time.*;
import lombok.Getter;
import lombok.Setter;

/** R05 持久化对象。 */
@Getter
@Setter
@TableName("eng_offline_command")
public class OfflineCommandEntity extends EngagementLogicalDeletionEntity {
  @TableId private Long id;
  private Long userId;
  private String deviceId;
  private String clientCommandId;
  private String commandType;
  private String requestDigest;
  private Long baseVersion;
  private String status;
  private String resultJson;
  private String errorCode;
  private LocalDateTime createdAt;
}
