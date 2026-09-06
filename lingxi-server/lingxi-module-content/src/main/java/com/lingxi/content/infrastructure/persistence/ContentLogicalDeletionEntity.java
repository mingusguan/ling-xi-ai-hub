package com.lingxi.content.infrastructure.persistence;

import com.baomidou.mybatisplus.annotation.TableLogic;
import lombok.Getter;
import lombok.Setter;

/** content 模块逻辑删除持久化基类。 */
@Getter
@Setter
public abstract class ContentLogicalDeletionEntity {
  @TableLogic(value = "0", delval = "1")
  private Boolean deleted;
}
