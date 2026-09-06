package com.lingxi.identity.infrastructure.persistence;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** 登录身份数据访问接口。 */
@Mapper
public interface LoginIdentityMapper extends BaseMapper<LoginIdentityEntity> {
  int tombstoneByUser(@Param("userId") long userId, @Param("requestId") long requestId);
}
