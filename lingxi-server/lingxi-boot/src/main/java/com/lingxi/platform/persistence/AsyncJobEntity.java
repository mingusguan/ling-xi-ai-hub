package com.lingxi.platform.persistence;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/** 可恢复异步任务持久化对象。 */
@Getter
@Setter
@TableName("plat_async_job")
public class AsyncJobEntity {
  @TableId private Long id;
  private String jobType;
  private String businessKey;
  private String status;
  private String payloadJson;
  private String resultJson;
  private Integer attemptCount;
  private Integer maxAttempts;
  private LocalDateTime nextRetryAt;
  private String leaseOwner;
  private LocalDateTime leaseUntil;
  private Integer progress;
  private String lastError;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
  private LocalDateTime completedAt;
  @Version private Long version;
}
