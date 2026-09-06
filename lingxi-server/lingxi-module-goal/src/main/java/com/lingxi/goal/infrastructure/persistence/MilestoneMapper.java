package com.lingxi.goal.infrastructure.persistence;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.util.List;
import org.apache.ibatis.annotations.Param;

/** 里程碑数据访问接口。 */
public interface MilestoneMapper extends BaseMapper<MilestoneEntity> {
  int insertBatch(@Param("items") List<MilestoneEntity> items);
}
