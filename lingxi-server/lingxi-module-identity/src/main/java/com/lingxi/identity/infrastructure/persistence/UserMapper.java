package com.lingxi.identity.infrastructure.persistence;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** 用户账号数据访问接口。 */
@Mapper
public interface UserMapper extends BaseMapper<UserEntity> {
  int tombstoneClosedUser(@Param("userId") long userId, @Param("requestId") long requestId);

  int countDeletedUser(@Param("userId") long userId);
}
