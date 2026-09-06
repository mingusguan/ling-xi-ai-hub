package com.lingxi.identity.infrastructure.persistence;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/** 用户设备持久化对象。 */
@Getter
@Setter
@TableName("id_device")
public class DeviceEntity extends IdentityLogicalDeletionEntity {
  @TableId private Long id;
  private Long userId;
  private String deviceId;
  private LocalDateTime firstSeenAt;
  private LocalDateTime lastSeenAt;
}
