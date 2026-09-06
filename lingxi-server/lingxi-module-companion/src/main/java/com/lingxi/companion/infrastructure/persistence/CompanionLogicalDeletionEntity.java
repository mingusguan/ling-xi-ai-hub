package com.lingxi.companion.infrastructure.persistence;

import com.baomidou.mybatisplus.annotation.TableLogic;
import lombok.Getter;
import lombok.Setter;

/** companion 模块逻辑删除持久化基类。 */
@Getter
@Setter
public abstract class CompanionLogicalDeletionEntity {
  @TableLogic(value = "0", delval = "1")
  private Boolean deleted;
}
