package com.lingxi.identity.api;

import com.lingxi.kernel.PageResult;
import java.time.LocalDateTime;

/** 用户账号的脱敏后台查询与受控状态管理。 */
public interface AdminUserFacade {
  PageResult<AdminUserSummary> listUsers(AdminUserQuery query);

  AdminUserSummary changeStatus(AdminUserStatusCommand command);

  record AdminUserQuery(String keyword, String ageBand, String status, int page, int pageSize) {}

  record AdminUserStatusCommand(
      long operatorAdminId,
      long userId,
      String targetStatus,
      String reason,
      String ticketNo,
      long expectedVersion,
      boolean recentAuthentication) {}

  record AdminUserSummary(
      long id,
      String publicId,
      String ageBand,
      String status,
      String timezone,
      long authorizationVersion,
      long version,
      LocalDateTime createdAt,
      LocalDateTime updatedAt) {}
}
