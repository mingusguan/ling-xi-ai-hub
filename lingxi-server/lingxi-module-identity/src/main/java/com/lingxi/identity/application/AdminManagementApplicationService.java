package com.lingxi.identity.application;

import com.lingxi.identity.api.*;
import com.lingxi.identity.domain.*;
import com.lingxi.kernel.*;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 管理员与 RBAC 用例，防止禁用最后一个超级管理员。 */
@Service
public class AdminManagementApplicationService implements AdminManagementFacade {
  private final AdminManagementRepository repository;
  private final AdminIdentityRepository identities;
  private final AdminAuthorizationFacade authorization;
  private final IdGenerator ids;
  private final DomainEventPublisher events;
  public AdminManagementApplicationService(AdminManagementRepository repository,
      AdminIdentityRepository identities, AdminAuthorizationFacade authorization, IdGenerator ids,DomainEventPublisher events){
    this.repository=repository;this.identities=identities;this.authorization=authorization;this.ids=ids;this.events=events;
  }
  public List<AdminSummary> admins(long operator){requireRead(operator);return repository.admins();}
  public List<RoleSummary> roles(long operator){requireRead(operator);return repository.roles();}
  public List<PermissionSummary> permissions(long operator){requireRead(operator);return repository.permissions();}

  @Transactional
  public AdminSummary createAdmin(CreateAdminCommand c){secure(c==null?null:c.context());
    if(c==null||c.roleIds()==null||c.roleIds().isEmpty()||!repository.rolesExist(c.roleIds()))invalid();
    if(repository.usernameExists(c.username()))throw new BusinessException("ADMIN_USERNAME_EXISTS","管理员账号已存在");
    LocalDateTime now=now();AdminAccount a=AdminAccount.create(ids.nextId(),c.username(),c.displayName(),AdminAuthenticationApplicationService.encodePassword(c.password()),now);
    repository.insertAccount(a);repository.replaceAdminRoles(a.getId(),c.roleIds(),now);audit(c.context(),"ADMIN_CREATE","ADMIN_ACCOUNT",a.getId(),a.getVersion());return find(a.getId());
  }

  @Transactional
  public AdminSummary changeAdminStatus(ChangeAdminStatusCommand c){secure(c==null?null:c.context());
    AdminAccount a=account(c.adminId());if(a.getVersion()!=c.expectedVersion())conflict();
    if(c.context().operatorId()==c.adminId()&&"DISABLED".equals(c.status()))throw new BusinessException("ADMIN_SELF_DISABLE_FORBIDDEN","不能停用当前登录账号");
    if("DISABLED".equals(c.status())&&repository.isSuperAdmin(c.adminId())&&repository.activeSuperAdminCount()<=1)throw new BusinessException("ADMIN_LAST_SUPER_FORBIDDEN","不能停用最后一个超级管理员");
    long old=a.getVersion();a.changeStatus(c.status(),now());if(!repository.updateManagedAccount(a,old))conflict();audit(c.context(),"ADMIN_STATUS_CHANGE","ADMIN_ACCOUNT",a.getId(),a.getVersion());return find(a.getId());
  }

  @Transactional
  public AdminSummary resetPassword(ResetAdminPasswordCommand c){secure(c==null?null:c.context());AdminAccount a=account(c.adminId());if(a.getVersion()!=c.expectedVersion())conflict();LocalDateTime changedAt=now();long old=a.getVersion();a.changePassword(AdminAuthenticationApplicationService.encodePassword(c.newPassword()),changedAt);if(!repository.updateManagedAccount(a,old))conflict();identities.revokeAllSessions(a.getId(),changedAt,"ADMIN_PASSWORD_RESET");audit(c.context(),"ADMIN_PASSWORD_RESET","ADMIN_ACCOUNT",a.getId(),a.getVersion());return find(a.getId());}

  @Transactional
  public AdminSummary assignRoles(AssignAdminRolesCommand c){secure(c==null?null:c.context());
    if(c.roleIds()==null||c.roleIds().isEmpty()||!repository.rolesExist(c.roleIds()))invalid();account(c.adminId());
    boolean removesSuper=repository.isSuperAdmin(c.adminId())&&!containsSuper(c.roleIds());
    if(removesSuper&&repository.activeSuperAdminCount()<=1)throw new BusinessException("ADMIN_LAST_SUPER_FORBIDDEN","不能移除最后一个超级管理员角色");
    repository.replaceAdminRoles(c.adminId(),c.roleIds(),now());audit(c.context(),"ADMIN_ROLES_ASSIGN","ADMIN_ACCOUNT",c.adminId(),0);return find(c.adminId());
  }

  @Transactional
  public RoleSummary createRole(CreateRoleCommand c){secure(c==null?null:c.context());
    if(c==null||c.roleKey()==null||!c.roleKey().matches("[A-Z][A-Z0-9_]{2,63}")||blank(c.name())||c.permissionIds()==null||!repository.permissionsExist(c.permissionIds()))invalid();
    if(repository.roleKeyExists(c.roleKey()))throw new BusinessException("ADMIN_ROLE_EXISTS","角色标识已存在");
    long roleId=ids.nextId();RoleSummary result=repository.insertRole(roleId,c.roleKey(),c.name().trim(),c.permissionIds(),now());audit(c.context(),"ADMIN_ROLE_CREATE","ADMIN_ROLE",roleId,0);return result;
  }

  @Transactional
  public RoleSummary setRolePermissions(SetRolePermissionsCommand c){secure(c==null?null:c.context());
    if(c==null||c.permissionIds()==null||!repository.permissionsExist(c.permissionIds()))invalid();
    RoleSummary role=repository.roles().stream().filter(x->x.id()==c.roleId()).findFirst().orElseThrow(()->new BusinessException("ADMIN_ROLE_NOT_FOUND","角色不存在"));
    if("SUPER_ADMIN".equals(role.roleKey()))throw new BusinessException("ADMIN_SUPER_ROLE_IMMUTABLE","超级管理员角色权限不可修改");
    RoleSummary result=repository.replaceRolePermissions(c.roleId(),c.permissionIds(),now());audit(c.context(),"ADMIN_ROLE_PERMISSIONS_CHANGE","ADMIN_ROLE",c.roleId(),0);return result;
  }

  private void secure(RiskContext c){if(c==null||c.operatorId()<=0||!c.recentAuthentication()||blank(c.reason())||blank(c.ticketNo()))throw new BusinessException("ADMIN_HIGH_RISK_CONTEXT_REQUIRED","操作需要近期认证、原因和工单号");requireRead(c.operatorId());}
  private void requireRead(long id){if(!authorization.allowed(id,"identity:admin:manage"))throw new BusinessException("ADMIN_FORBIDDEN","管理员权限不足");}
  private AdminAccount account(long id){return identities.findById(id).orElseThrow(()->new BusinessException("ADMIN_NOT_FOUND","管理员不存在"));}
  private AdminSummary find(long id){return repository.admins().stream().filter(x->x.id()==id).findFirst().orElseThrow();}
  private boolean containsSuper(Set<Long> roleIds){return repository.roles().stream().anyMatch(r->roleIds.contains(r.id())&&"SUPER_ADMIN".equals(r.roleKey()));}
  private void audit(RiskContext c,String action,String type,long id,long version){events.publish(new AdminManagementAuditedEvent(java.util.UUID.randomUUID().toString(),c.operatorId(),action,type,String.valueOf(id),version,c.reason(),c.ticketNo(),java.time.Instant.now()));}
  private void invalid(){throw new BusinessException("ADMIN_MANAGEMENT_INVALID","管理参数不合法");}private void conflict(){throw new BusinessException("ADMIN_CONCURRENT_UPDATE","管理员账号已变化");}
  private boolean blank(String v){return v==null||v.isBlank();}private LocalDateTime now(){return LocalDateTime.now(ZoneOffset.UTC);}
}
