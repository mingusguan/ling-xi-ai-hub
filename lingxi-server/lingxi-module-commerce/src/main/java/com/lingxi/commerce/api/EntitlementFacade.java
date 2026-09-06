package com.lingxi.commerce.api;

public interface EntitlementFacade {
  EntitlementResult get(long userId, String resourceKey);

  EntitlementResult consume(ConsumeEntitlementCommand command);
}
