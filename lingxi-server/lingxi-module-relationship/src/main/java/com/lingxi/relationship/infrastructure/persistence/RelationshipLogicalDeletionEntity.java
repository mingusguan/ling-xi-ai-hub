package com.lingxi.relationship.infrastructure.persistence;

import com.baomidou.mybatisplus.annotation.TableLogic;
import lombok.Getter;
import lombok.Setter;

/** relationship 模块逻辑删除持久化基类。 */
@Getter
@Setter
public abstract class RelationshipLogicalDeletionEntity {
  @TableLogic(value = "0", delval = "1")
  private Boolean deleted;
}
