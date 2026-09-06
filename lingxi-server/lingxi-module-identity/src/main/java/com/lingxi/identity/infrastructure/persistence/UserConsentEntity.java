package com.lingxi.identity.infrastructure.persistence;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/** 不可覆盖的用户协议证据。 */
@Getter
@Setter
@TableName("id_user_consent")
public class UserConsentEntity extends IdentityLogicalDeletionEntity {
  @TableId private Long id;
  private Long userId;
  private String purpose;
  private String documentVersion;
  private Boolean granted;
  private String evidenceReference;
  private LocalDateTime recordedAt;
}
