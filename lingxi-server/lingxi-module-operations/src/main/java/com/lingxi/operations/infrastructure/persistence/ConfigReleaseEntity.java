package com.lingxi.operations.infrastructure.persistence;

import com.baomidou.mybatisplus.annotation.*;
import java.time.*;
import lombok.*;

@Getter
@Setter
@TableName("ops_config_release")
public class ConfigReleaseEntity {
  @TableId private Long id;
  private String releaseKey, configType;
  private Integer versionNo;
  private String contentRef, contentDigest, grayRule, status;
  private Long createdBy, approvedBy, publishedBy, previousReleaseId;
  private String failureReason;
  @Version private Long lockVersion;
  private LocalDateTime createdAt, updatedAt;
}
