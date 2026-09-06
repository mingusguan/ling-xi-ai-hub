package com.lingxi.identity.api;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/** 管理员账号和 RBAC 管理门面。 */
public interface AdminManagementFacade {
  List<AdminSummary> admins(long operatorId);
  List<RoleSummary> roles(long operatorId);
  List<PermissionSummary> permissions(long operatorId);
  AdminSummary createAdmin(CreateAdminCommand command);
  AdminSummary changeAdminStatus(ChangeAdminStatusCommand command);
  AdminSummary resetPassword(ResetAdminPasswordCommand command);
  AdminSummary assignRoles(AssignAdminRolesCommand command);
  RoleSummary createRole(CreateRoleCommand command);
  RoleSummary setRolePermissions(SetRolePermissionsCommand command);

  record RiskContext(long operatorId, boolean recentAuthentication, String reason, String ticketNo) {}
  record CreateAdminCommand(String username, String displayName, String password,
      Set<Long> roleIds, RiskContext context) {}
  record ChangeAdminStatusCommand(long adminId, String status, long expectedVersion, RiskContext context) {}
  record ResetAdminPasswordCommand(long adminId, String newPassword, long expectedVersion, RiskContext context) {}
  record AssignAdminRolesCommand(long adminId, Set<Long> roleIds, RiskContext context) {}
  record CreateRoleCommand(String roleKey, String name, Set<Long> permissionIds, RiskContext context) {}
  record SetRolePermissionsCommand(long roleId, Set<Long> permissionIds, RiskContext context) {}
  record AdminSummary(long id, String username, String displayName, String status,
      long version, LocalDateTime lastLoginAt, Set<RoleBrief> roles, LocalDateTime createdAt) {}
  record RoleBrief(long id, String roleKey, String name) {}
  record RoleSummary(long id, String roleKey, String name, String status,
      Set<PermissionSummary> permissions) {}
  record PermissionSummary(long id, String permissionKey, String name,
      String permissionGroup, String riskLevel) {}
}
