package com.lingxi.commerce.infrastructure.persistence;

import com.baomidou.mybatisplus.annotation.TableLogic;
import lombok.Getter;
import lombok.Setter;

/** commerce 模块逻辑删除持久化基类。 */
@Getter
@Setter
public abstract class CommerceLogicalDeletionEntity {
  @TableLogic(value = "0", delval = "1")
  private Boolean deleted;
}
