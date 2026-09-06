package com.lingxi.identity.infrastructure.persistence;

import com.baomidou.mybatisplus.annotation.*;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/** 加密隐私导出包。 */
@Getter
@Setter
@TableName("id_privacy_export")
public class PrivacyExportEntity extends IdentityLogicalDeletionEntity {
  @TableId private Long requestId;
  private Long userId;
  private byte[] nonce;
  private byte[] cipherText;
  private LocalDateTime expiresAt;
  private LocalDateTime createdAt;
}
