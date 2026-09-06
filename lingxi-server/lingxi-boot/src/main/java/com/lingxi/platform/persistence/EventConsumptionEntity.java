package com.lingxi.platform.persistence;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/** 单个消费者的事件幂等凭证。 */
@Getter
@Setter
@TableName("plat_event_consumption")
public class EventConsumptionEntity {

  @TableId private Long id;
  private String eventId;
  private String consumerName;
  private LocalDateTime consumedAt;
}
