package com.lingxi.commerce.api;

import java.util.List;

public interface EntitlementFacade {
  EntitlementResult get(long userId, String resourceKey);

  /** 查询本人全部权益余额，供客户端一次展示会员权益总览。 */
  List<EntitlementResult> list(long userId);

  EntitlementResult consume(ConsumeEntitlementCommand command);
}
