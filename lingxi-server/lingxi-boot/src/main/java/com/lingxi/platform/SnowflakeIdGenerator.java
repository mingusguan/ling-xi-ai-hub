package com.lingxi.platform;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.lingxi.kernel.IdGenerator;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** 单体应用统一标识生成器。数据库主键趋势递增，公开标识不可枚举。 */
@Component
public class SnowflakeIdGenerator implements IdGenerator {

  @Override
  public long nextId() {
    return IdWorker.getId();
  }

  @Override
  public String nextPublicId() {
    return UUID.randomUUID().toString().replace("-", "");
  }

  @Override
  public String nextEventId() {
    return UUID.randomUUID().toString();
  }
}
