package com.lingxi.goal.infrastructure.persistence;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.util.List;
import org.apache.ibatis.annotations.Param;

/** 行动定义数据访问接口。 */
public interface ActionMapper extends BaseMapper<ActionEntity> {
  int insertBatch(@Param("items") List<ActionEntity> items);
}
