package com.lingxi.wiring;

import com.lingxi.commerce.api.EntitlementFacade;
import com.lingxi.goal.api.ActiveGoalQuota;
import com.lingxi.goal.api.ActiveGoalQuotaPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 活跃目标配额的组合根装配。
 *
 * <p>目标模块只声明 {@link ActiveGoalQuotaPort} 端口，不直接依赖交易模块；
 * 上限来自会员权益余额，因此后台可以通过权益调整给单个用户放宽，不需要发版。
 * 未持有权益或余额不高于免费档时，取免费档上限。
 */
@Configuration
public class ActiveGoalQuotaConfiguration {

  @Bean
  public ActiveGoalQuotaPort activeGoalQuotaPort(EntitlementFacade entitlements) {
    return userId ->
        ActiveGoalQuota.allowanceFromEntitlement(
            entitlements.get(userId, ActiveGoalQuota.ENTITLEMENT_RESOURCE_KEY).balance());
  }
}
