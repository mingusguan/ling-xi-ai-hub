package com.lingxi.identity.infrastructure.persistence;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** 会话数据访问接口。 */
@Mapper
public interface SessionMapper extends BaseMapper<SessionEntity> {
  int tombstoneByUser(@Param("userId") long userId, @Param("requestId") long requestId);
}
