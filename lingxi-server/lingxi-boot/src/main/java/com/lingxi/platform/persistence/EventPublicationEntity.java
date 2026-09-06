package com.lingxi.platform.persistence;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/** 持久化事件发布记录。 */
@Getter
@Setter
@TableName("plat_event_publication")
public class EventPublicationEntity {

  @TableId private Long id;
  private String eventId;
  private String eventType;
  private String aggregateId;
  private Long aggregateVersion;
  private Integer schemaVersion;
  private String payloadJson;
  private String status;
  private Integer attemptCount;
  private LocalDateTime nextRetryAt;
  private LocalDateTime leaseUntil;
  private String lastError;
  private LocalDateTime occurredAt;
  private LocalDateTime createdAt;
  private LocalDateTime completedAt;
}
