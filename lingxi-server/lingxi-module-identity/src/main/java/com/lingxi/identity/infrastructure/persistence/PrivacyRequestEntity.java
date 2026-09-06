package com.lingxi.identity.infrastructure.persistence;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/** 隐私请求持久化对象。 */
@Getter
@Setter
@TableName("id_privacy_request")
public class PrivacyRequestEntity {
  @TableId private Long id;
  private String requestKey;
  private String requestDigest;
  private Long userId;
  private String type;
  private String scopeJson;
  private String status;
  private Integer progress;
  private LocalDateTime deadline;
  private String resultReference;
  private String lastError;
  @Version private Long version;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
}
