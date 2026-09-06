package com.lingxi.relationship.infrastructure.persistence;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.time.*;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ShareLinkMapper extends BaseMapper<ShareLinkEntity> {
  int consume(
      @Param("id") long id, @Param("now") Instant now, @Param("updatedAt") LocalDateTime updatedAt);
}
