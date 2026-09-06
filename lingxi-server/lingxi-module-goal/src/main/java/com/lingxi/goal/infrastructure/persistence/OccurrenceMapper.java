package com.lingxi.goal.infrastructure.persistence;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.util.List;
import org.apache.ibatis.annotations.Param;

/** 行动实例数据访问接口。 */
public interface OccurrenceMapper extends BaseMapper<OccurrenceEntity> {
  int insertIgnoreBatch(@Param("items") List<OccurrenceEntity> items);
}
