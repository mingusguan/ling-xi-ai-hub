package com.lingxi.platform.persistence;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** 异步任务数据访问接口。 */
@Mapper
public interface AsyncJobMapper extends BaseMapper<AsyncJobEntity> {

  List<AsyncJobEntity> selectByBusinessKeys(@Param("jobs") List<AsyncJobEntity> jobs);

  int insertBatch(@Param("jobs") List<AsyncJobEntity> jobs);
}
