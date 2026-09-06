package com.lingxi.commerce.infrastructure.persistence;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.time.*;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface EntitlementMapper extends BaseMapper<EntitlementEntity> {
  int grant(
      @Param("id") long id,
      @Param("user") long user,
      @Param("resource") String resource,
      @Param("delta") long delta,
      @Param("expires") Instant expires,
      @Param("now") LocalDateTime now);

  int consume(
      @Param("user") long user,
      @Param("resource") String resource,
      @Param("amount") long amount,
      @Param("now") LocalDateTime now);
}
