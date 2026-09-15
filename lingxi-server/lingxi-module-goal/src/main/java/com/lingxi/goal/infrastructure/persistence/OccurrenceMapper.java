package com.lingxi.goal.infrastructure.persistence;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.util.List;
import org.apache.ibatis.annotations.Param;

/** 行动实例数据访问接口。 */
public interface OccurrenceMapper extends BaseMapper<OccurrenceEntity> {
  /** 按 (action_id, scheduled_at) 幂等重建实例；已存在且未执行的实例会被重新物化。 */
  int upsertBatch(@Param("items") List<OccurrenceEntity> items);
}
