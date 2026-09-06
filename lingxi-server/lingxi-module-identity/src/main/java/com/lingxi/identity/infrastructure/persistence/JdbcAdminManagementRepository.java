package com.lingxi.identity.infrastructure.persistence;

import com.lingxi.identity.api.AdminManagementFacade.*;
import com.lingxi.identity.domain.AdminAccount;
import com.lingxi.identity.domain.AdminManagementRepository;
import com.lingxi.kernel.IdGenerator;
import java.time.LocalDateTime;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** RBAC 持久化适配器，关系调整统一使用逻辑删除和恢复。 */
@Repository
public class JdbcAdminManagementRepository implements AdminManagementRepository {
  private final JdbcTemplate jdbc;
  private final IdGenerator ids;
  public JdbcAdminManagementRepository(JdbcTemplate jdbc, IdGenerator ids){this.jdbc=jdbc;this.ids=ids;}

  public List<AdminSummary> admins(){
    Map<Long,RoleBrief> roles=new HashMap<>();
    jdbc.query("SELECT id,role_key,name FROM id_admin_role WHERE deleted=0",rs->{roles.put(rs.getLong("id"),new RoleBrief(rs.getLong("id"),rs.getString("role_key"),rs.getString("name")));});
    Map<Long,Set<RoleBrief>> rolesByAdmin=new HashMap<>();
    jdbc.query("SELECT admin_id,role_id FROM id_admin_account_role WHERE deleted=0",rs->{
      RoleBrief role=roles.get(rs.getLong("role_id"));if(role!=null)rolesByAdmin.computeIfAbsent(rs.getLong("admin_id"),x->new LinkedHashSet<>()).add(role);
    });
    return jdbc.query("SELECT id,username,display_name,status,version,last_login_at,created_at FROM id_admin_account WHERE deleted=0 ORDER BY created_at DESC",(rs,row)->
        new AdminSummary(rs.getLong("id"),rs.getString("username"),rs.getString("display_name"),rs.getString("status"),rs.getLong("version"),
            rs.getObject("last_login_at",LocalDateTime.class),Set.copyOf(rolesByAdmin.getOrDefault(rs.getLong("id"),Set.of())),rs.getObject("created_at",LocalDateTime.class)));
  }

  public List<RoleSummary> roles(){
    Map<Long,Set<PermissionSummary>> byRole=permissionsByRole();
    return jdbc.query("SELECT id,role_key,name,status FROM id_admin_role WHERE deleted=0 ORDER BY id",(rs,row)->
        new RoleSummary(rs.getLong("id"),rs.getString("role_key"),rs.getString("name"),rs.getString("status"),
            Set.copyOf(byRole.getOrDefault(rs.getLong("id"),Set.of()))));
  }

  public List<PermissionSummary> permissions(){
    return jdbc.query("SELECT id,permission_key,name,permission_group,risk_level FROM id_admin_permission WHERE status='ACTIVE' ORDER BY permission_group,id",(rs,row)->permission(rs));
  }

  public void insertAccount(AdminAccount a){
    jdbc.update("INSERT INTO id_admin_account(id,username,display_name,password_hash,status,failed_attempts,locked_until,password_changed_at,last_login_at,version,deleted,created_at,updated_at) VALUES(?,?,?,?,?,?,?,?,?,?,0,?,?)",
        a.getId(),a.getUsername(),a.getDisplayName(),a.getPasswordHash(),a.getStatus(),a.getFailedAttempts(),a.getLockedUntil(),a.getPasswordChangedAt(),a.getLastLoginAt(),a.getVersion(),a.getCreatedAt(),a.getUpdatedAt());
  }

  public boolean updateManagedAccount(AdminAccount a,long previous){
    return jdbc.update("UPDATE id_admin_account SET password_hash=?,status=?,failed_attempts=?,locked_until=?,password_changed_at=?,version=?,updated_at=? WHERE id=? AND version=? AND deleted=0",
        a.getPasswordHash(),a.getStatus(),a.getFailedAttempts(),a.getLockedUntil(),a.getPasswordChangedAt(),a.getVersion(),a.getUpdatedAt(),a.getId(),previous)==1;
  }

  public void replaceAdminRoles(long adminId,Set<Long> roleIds,LocalDateTime now){
    jdbc.update("UPDATE id_admin_account_role SET deleted=1,deleted_at=? WHERE admin_id=? AND deleted=0",now,adminId);
    List<Object[]> rows=roleIds.stream().map(roleId->new Object[]{ids.nextId(),adminId,roleId,now}).toList();
    if(!rows.isEmpty())jdbc.batchUpdate("INSERT INTO id_admin_account_role(id,admin_id,role_id,deleted,deleted_at,created_at) VALUES(?,?,?,0,NULL,?) ON DUPLICATE KEY UPDATE deleted=0,deleted_at=NULL",rows);
  }

  public RoleSummary insertRole(long id,String key,String name,Set<Long> permissionIds,LocalDateTime now){
    jdbc.update("INSERT INTO id_admin_role(id,role_key,name,status,deleted,created_at,updated_at) VALUES(?,?,?,'ACTIVE',0,?,?)",id,key,name,now,now);
    replaceRolePermissions(id,permissionIds,now);return role(id);
  }

  public RoleSummary replaceRolePermissions(long roleId,Set<Long> permissionIds,LocalDateTime now){
    jdbc.update("UPDATE id_admin_role_permission SET deleted=1,deleted_at=? WHERE role_id=? AND deleted=0",now,roleId);
    List<Object[]> rows=permissionIds.stream().map(permissionId->new Object[]{ids.nextId(),roleId,permissionId,now}).toList();
    if(!rows.isEmpty())jdbc.batchUpdate("INSERT INTO id_admin_role_permission(id,role_id,permission_id,deleted,deleted_at,created_at) VALUES(?,?,?,0,NULL,?) ON DUPLICATE KEY UPDATE deleted=0,deleted_at=NULL",rows);
    return role(roleId);
  }

  public boolean usernameExists(String username){return count("SELECT COUNT(*) FROM id_admin_account WHERE username=? AND deleted=0",username)>0;}
  public boolean roleKeyExists(String key){return count("SELECT COUNT(*) FROM id_admin_role WHERE role_key=? AND deleted=0",key)>0;}
  public boolean rolesExist(Set<Long> roleIds){return roleIds!=null&&countIn("id_admin_role",roleIds)==roleIds.size();}
  public boolean permissionsExist(Set<Long> ids){return ids!=null&&countIn("id_admin_permission",ids)==ids.size();}
  public boolean isSuperAdmin(long adminId){Long roleId=superRoleId();return roleId!=null&&count("SELECT COUNT(*) FROM id_admin_account_role WHERE admin_id=? AND role_id=? AND deleted=0",adminId,roleId)>0;}
  public long activeSuperAdminCount(){Long roleId=superRoleId();if(roleId==null)return 0;List<Long> adminIds=jdbc.query("SELECT admin_id FROM id_admin_account_role WHERE role_id=? AND deleted=0",(rs,row)->rs.getLong("admin_id"),roleId);if(adminIds.isEmpty())return 0;String marks=String.join(",",Collections.nCopies(adminIds.size(),"?"));return count("SELECT COUNT(*) FROM id_admin_account WHERE id IN ("+marks+") AND deleted=0 AND status='ACTIVE'",adminIds.toArray());}

  private RoleSummary role(long id){
    Map<Long,Set<PermissionSummary>> p=permissionsByRole();
    return jdbc.query("SELECT id,role_key,name,status FROM id_admin_role WHERE id=? AND deleted=0",(rs,row)->new RoleSummary(rs.getLong("id"),rs.getString("role_key"),rs.getString("name"),rs.getString("status"),Set.copyOf(p.getOrDefault(id,Set.of()))),id).stream().findFirst().orElseThrow();
  }
  private Map<Long,Set<PermissionSummary>> permissionsByRole(){Map<Long,PermissionSummary> permissions=new HashMap<>();jdbc.query("SELECT id,permission_key,name,permission_group,risk_level FROM id_admin_permission WHERE status='ACTIVE'",rs->{PermissionSummary p=permission(rs);permissions.put(p.id(),p);});Map<Long,Set<PermissionSummary>> map=new HashMap<>();jdbc.query("SELECT role_id,permission_id FROM id_admin_role_permission WHERE deleted=0",rs->{PermissionSummary p=permissions.get(rs.getLong("permission_id"));if(p!=null)map.computeIfAbsent(rs.getLong("role_id"),x->new LinkedHashSet<>()).add(p);});return map;}
  private Long superRoleId(){return jdbc.query("SELECT id FROM id_admin_role WHERE role_key='SUPER_ADMIN' AND deleted=0",(rs,row)->rs.getLong("id")).stream().findFirst().orElse(null);}
  private PermissionSummary permission(java.sql.ResultSet rs)throws java.sql.SQLException{return new PermissionSummary(rs.getLong("id"),rs.getString("permission_key"),rs.getString("name"),rs.getString("permission_group"),rs.getString("risk_level"));}
  private long count(String sql,Object...args){Long v=jdbc.queryForObject(sql,Long.class,args);return v==null?0:v;}
  private long countIn(String table,Set<Long> values){if(values.isEmpty())return 0;String marks=String.join(",",Collections.nCopies(values.size(),"?"));String extra="id_admin_role".equals(table)?" AND deleted=0 AND status='ACTIVE'":" AND status='ACTIVE'";return count("SELECT COUNT(*) FROM "+table+" WHERE id IN ("+marks+")"+extra,values.toArray());}
}
