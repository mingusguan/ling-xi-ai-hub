package com.lingxi.operations.infrastructure.security;

import com.lingxi.identity.api.AdminAuthorizationFacade;
import com.lingxi.operations.application.AdminPermissionAdapter;
import org.springframework.stereotype.Component;

/** operations 只消费 identity 暴露的管理员权限事实。 */
@Component
public class IdentityAdminPermissionAdapter implements AdminPermissionAdapter {
  private final AdminAuthorizationFacade admins;

  public IdentityAdminPermissionAdapter(AdminAuthorizationFacade admins) { this.admins = admins; }

  @Override
  public boolean allowed(long adminId, String permission) {
    return admins.allowed(adminId, permission);
  }
}
