package com.lingxi.identity.infrastructure.persistence;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/** 登录身份持久化对象。 */
@Getter
@Setter
@TableName("id_login_identity")
public class LoginIdentityEntity extends IdentityLogicalDeletionEntity {
  @TableId private Long id;
  private Long userId;
  private String channel;
  private String subjectHash;
  private LocalDateTime verifiedAt;
  private LocalDateTime createdAt;
}
