package com.lingxi.commerce.infrastructure.persistence;

import com.baomidou.mybatisplus.annotation.*;
import java.time.*;
import lombok.*;

@Getter
@Setter
@TableName("pay_channel_callback")
public class ChannelCallbackEntity extends CommerceLogicalDeletionEntity {
  @TableId private Long id;
  private String channel, callbackId, digest, status;
  private LocalDateTime receivedAt, updatedAt;
}
