package com.lingxi.relationship.infrastructure.persistence;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.util.List;
import org.apache.ibatis.annotations.Param;

/** 监护权限数据访问接口。 */
public interface GuardianPermissionMapper extends BaseMapper<GuardianPermissionEntity> {
  /** 批量插入最小监护权限。 */
  int insertBatch(@Param("permissions") List<GuardianPermissionEntity> permissions);
}
