package com.lingxi.identity.infrastructure.persistence;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PrivacyExportMapper extends BaseMapper<PrivacyExportEntity> {

  /** 同一隐私请求重试时原子替换加密临时包，并恢复其逻辑有效状态。 */
  int upsert(PrivacyExportEntity entity);
}
