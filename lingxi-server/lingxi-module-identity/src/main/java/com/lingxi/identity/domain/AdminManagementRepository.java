package com.lingxi.identity.domain;

import com.lingxi.identity.api.AdminManagementFacade.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/** 管理员与 RBAC 维护端口。 */
public interface AdminManagementRepository {
  List<AdminSummary> admins();
  List<RoleSummary> roles();
  List<PermissionSummary> permissions();
  void insertAccount(AdminAccount account);
  boolean updateManagedAccount(AdminAccount account, long previousVersion);
  void replaceAdminRoles(long adminId, Set<Long> roleIds, LocalDateTime now);
  RoleSummary insertRole(long id, String roleKey, String name, Set<Long> permissionIds, LocalDateTime now);
  RoleSummary replaceRolePermissions(long roleId, Set<Long> permissionIds, LocalDateTime now);
  boolean usernameExists(String username);
  boolean roleKeyExists(String roleKey);
  boolean rolesExist(Set<Long> roleIds);
  boolean permissionsExist(Set<Long> permissionIds);
  boolean isSuperAdmin(long adminId);
  long activeSuperAdminCount();
}
