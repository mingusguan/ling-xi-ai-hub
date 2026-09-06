package com.lingxi.goal.infrastructure.persistence;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.util.List;
import org.apache.ibatis.annotations.Param;

/** 复盘数据访问接口。 */
public interface ReviewMapper extends BaseMapper<ReviewEntity> {
  int insertIgnoreBatch(@Param("items") List<ReviewEntity> items);
}
