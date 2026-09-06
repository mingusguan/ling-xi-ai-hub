package com.lingxi.identity.interfaces;

import com.lingxi.identity.api.AdminManagementFacade;
import com.lingxi.identity.api.AdminManagementFacade.*;
import com.lingxi.kernel.*;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Set;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin-api/v1/admin-management")
public class AdminManagementController {
  private final AdminManagementFacade facade;
  public AdminManagementController(AdminManagementFacade facade){this.facade=facade;}
  @GetMapping("/admins")public ApiResponse<?> admins(HttpServletRequest r){return ok(facade.admins(admin().actorId()),r);}
  @GetMapping("/roles")public ApiResponse<?> roles(HttpServletRequest r){return ok(facade.roles(admin().actorId()),r);}
  @GetMapping("/permissions")public ApiResponse<?> permissions(HttpServletRequest r){return ok(facade.permissions(admin().actorId()),r);}
  @PostMapping("/admins")public ApiResponse<?> create(@RequestBody CreateBody b,@RequestHeader("X-Operation-Reason")String reason,@RequestHeader("X-Ticket-No")String ticket,HttpServletRequest r){return ok(facade.createAdmin(new CreateAdminCommand(b.username(),b.displayName(),b.password(),b.roleIds(),context(reason,ticket))),r);}
  @PutMapping("/admins/{id}/status")public ApiResponse<?> status(@PathVariable long id,@RequestBody StatusBody b,@RequestHeader("X-Operation-Reason")String reason,@RequestHeader("X-Ticket-No")String ticket,HttpServletRequest r){return ok(facade.changeAdminStatus(new ChangeAdminStatusCommand(id,b.status(),b.expectedVersion(),context(reason,ticket))),r);}
  @PutMapping("/admins/{id}/password")public ApiResponse<?> password(@PathVariable long id,@RequestBody PasswordBody b,@RequestHeader("X-Operation-Reason")String reason,@RequestHeader("X-Ticket-No")String ticket,HttpServletRequest r){return ok(facade.resetPassword(new ResetAdminPasswordCommand(id,b.newPassword(),b.expectedVersion(),context(reason,ticket))),r);}
  @PutMapping("/admins/{id}/roles")public ApiResponse<?> assign(@PathVariable long id,@RequestBody RoleIdsBody b,@RequestHeader("X-Operation-Reason")String reason,@RequestHeader("X-Ticket-No")String ticket,HttpServletRequest r){return ok(facade.assignRoles(new AssignAdminRolesCommand(id,b.roleIds(),context(reason,ticket))),r);}
  @PostMapping("/roles")public ApiResponse<?> role(@RequestBody RoleBody b,@RequestHeader("X-Operation-Reason")String reason,@RequestHeader("X-Ticket-No")String ticket,HttpServletRequest r){return ok(facade.createRole(new CreateRoleCommand(b.roleKey(),b.name(),b.permissionIds(),context(reason,ticket))),r);}
  @PutMapping("/roles/{id}/permissions")public ApiResponse<?> permissions(@PathVariable long id,@RequestBody PermissionIdsBody b,@RequestHeader("X-Operation-Reason")String reason,@RequestHeader("X-Ticket-No")String ticket,HttpServletRequest r){return ok(facade.setRolePermissions(new SetRolePermissionsCommand(id,b.permissionIds(),context(reason,ticket))),r);}
  private ActorContext admin(){return ActorContextHolder.requireAdmin();}private RiskContext context(String reason,String ticket){ActorContext a=admin();return new RiskContext(a.actorId(),a.recentAuthentication(),reason,ticket);}private <T>ApiResponse<T>ok(T v,HttpServletRequest r){return ApiResponse.success(v,String.valueOf(r.getAttribute(RequestAttributes.REQUEST_ID)));}
  public record CreateBody(String username,String displayName,String password,Set<Long>roleIds){}public record StatusBody(String status,long expectedVersion){}public record PasswordBody(String newPassword,long expectedVersion){}public record RoleIdsBody(Set<Long>roleIds){}public record RoleBody(String roleKey,String name,Set<Long>permissionIds){}public record PermissionIdsBody(Set<Long>permissionIds){}
}
