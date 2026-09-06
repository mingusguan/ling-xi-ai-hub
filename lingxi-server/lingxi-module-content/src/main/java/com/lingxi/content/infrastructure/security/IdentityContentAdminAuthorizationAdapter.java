package com.lingxi.content.infrastructure.security;

import com.lingxi.content.application.ContentAdminAuthorizationAdapter;
import com.lingxi.identity.api.AdminAuthorizationFacade;
import org.springframework.stereotype.Component;

/** 内容后台权限由 identity 的独立 Admin RBAC 提供。 */
@Component
public class IdentityContentAdminAuthorizationAdapter implements ContentAdminAuthorizationAdapter {
  private final AdminAuthorizationFacade admins;

  public IdentityContentAdminAuthorizationAdapter(AdminAuthorizationFacade admins) {
    this.admins = admins;
  }

  @Override
  public boolean allowed(long adminId, String permission) {
    return admins.allowed(adminId, permission);
  }
}
