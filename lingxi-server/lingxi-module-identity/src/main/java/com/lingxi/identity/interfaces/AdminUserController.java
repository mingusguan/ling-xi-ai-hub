package com.lingxi.identity.interfaces;

import com.lingxi.identity.api.AdminUserFacade;
import com.lingxi.kernel.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

/** 用户与青少年账号后台接口，响应不包含私密目标或对话正文。 */
@RestController
@RequestMapping("/admin-api/v1/users")
public class AdminUserController {
  private final AdminUserFacade facade;

  public AdminUserController(AdminUserFacade facade) { this.facade = facade; }

  @GetMapping
  public ApiResponse<PageResult<AdminUserFacade.AdminUserSummary>> list(
      @RequestParam(required = false) String keyword,
      @RequestParam(required = false) String ageBand,
      @RequestParam(required = false) String status,
      @RequestParam(defaultValue = "1") int page,
      @RequestParam(defaultValue = "20") int pageSize,
      HttpServletRequest request) {
    return ok(facade.listUsers(new AdminUserFacade.AdminUserQuery(
        keyword, ageBand, status, page, pageSize)), request);
  }

  @PutMapping("/{id}/status")
  public ApiResponse<AdminUserFacade.AdminUserSummary> status(
      @PathVariable long id,
      @RequestHeader("X-Operation-Reason") String reason,
      @RequestHeader("X-Ticket-No") String ticketNo,
      @RequestBody StatusBody body,
      HttpServletRequest request) {
    ActorContext actor = ActorContextHolder.requireAdmin();
    return ok(facade.changeStatus(new AdminUserFacade.AdminUserStatusCommand(
        actor.actorId(), id, body.status(), reason, ticketNo, body.expectedVersion(),
        actor.recentAuthentication())), request);
  }

  private <T> ApiResponse<T> ok(T value, HttpServletRequest request) {
    return ApiResponse.success(value,
        String.valueOf(request.getAttribute(RequestAttributes.REQUEST_ID)));
  }

  public record StatusBody(String status, long expectedVersion) {}
}
