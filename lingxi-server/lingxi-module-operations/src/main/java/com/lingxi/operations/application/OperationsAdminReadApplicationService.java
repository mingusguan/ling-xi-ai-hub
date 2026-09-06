package com.lingxi.operations.application;

import com.lingxi.commerce.api.AdminCommerceFacade;
import com.lingxi.identity.api.AdminAuthorizationFacade;
import com.lingxi.identity.api.AdminUserFacade;
import com.lingxi.kernel.*;
import com.lingxi.operations.api.OperationsAdminReadFacade;
import com.lingxi.operations.domain.OperationsAdminReadRepository;
import org.springframework.stereotype.Service;

/** 组合各模块公开的后台读门面，不跨模块查询数据库。 */
@Service
public class OperationsAdminReadApplicationService implements OperationsAdminReadFacade {
  private final OperationsAdminReadRepository reads;
  private final AdminAuthorizationFacade admins;
  private final AdminCommerceFacade commerce;
  private final AdminUserFacade users;

  public OperationsAdminReadApplicationService(OperationsAdminReadRepository reads,
      AdminAuthorizationFacade admins, AdminCommerceFacade commerce, AdminUserFacade users) {
    this.reads=reads;this.admins=admins;this.commerce=commerce;this.users=users;
  }

  public DashboardSnapshot dashboard(long adminId){
    require(adminId,"dashboard:read");DashboardSnapshot local=reads.localDashboard();
    long orders=0,paid=0,revenue=0,subscriptions=0,userCount=0,teens=0;
    if(admins.allowed(adminId,"commerce:read")){
      var c=commerce.overview(adminId);orders=c.orderCount();paid=c.paidOrderCount();revenue=c.revenueMinor();subscriptions=c.activeSubscriptions();
    }
    if(admins.allowed(adminId,"identity:user:read")){
      userCount=users.listUsers(new AdminUserFacade.AdminUserQuery(null,null,null,1,1)).total();
      teens=users.listUsers(new AdminUserFacade.AdminUserQuery(null,"TEEN",null,1,1)).total();
    }
    return new DashboardSnapshot(local.openTickets(),local.pendingReleases(),local.highRiskCases(),
        local.auditActionsToday(),orders,paid,revenue,subscriptions,userCount,teens);
  }

  public PageResult<TicketSummary> tickets(long id,String status,String category,int page,int size){
    require(id,"support.ticket.manage");return reads.tickets(status,category,page(page),size(size));
  }
  public PageResult<ReleaseSummary> releases(long id,String type,String status,int page,int size){
    requireAny(id,"config.release.create","config.release.validate","config.release.approve","config.release.publish");
    return reads.releases(type,status,page(page),size(size));
  }
  public PageResult<AuditSummary> audits(long id,String action,Long operator,int page,int size){
    require(id,"audit:read");return reads.audits(action,operator,page(page),size(size));
  }
  public PageResult<SafetyCaseSummary> safetyCases(long id,String status,String level,int page,int size){
    require(id,"safety:case:manage");return reads.safetyCases(status,level,page(page),size(size));
  }
  public PageResult<ResourceSummary> resources(long id,String type,String status,int page,int size){
    String permission=switch(type==null?"":type){
      case "CAMPAIGN"->"message:campaign:manage";
      case "EXPERIMENT"->"experiment:manage";
      case "APP_RELEASE"->"app:release:manage";
      case "COMPLIANCE"->"compliance:manage";
      case "FEATURE_FLAG"->"feature:flag:manage";
      default->throw new BusinessException("OPS_RESOURCE_TYPE_INVALID","运营资源类型不合法");};
    require(id,permission);return reads.resources(type,status,page(page),size(size));
  }
  public TicketDetail ticketDetail(long id,long ticketId){requireAny(id,"support.ticket.manage","support:ticket:reply");return reads.ticketDetail(ticketId);}
  public java.util.List<MetricSeries> metrics(long id,String key,String dimension,java.time.LocalDateTime from,java.time.LocalDateTime to){require(id,"analytics:read");java.time.LocalDateTime end=to==null?java.time.LocalDateTime.now(java.time.ZoneOffset.UTC):to;java.time.LocalDateTime start=from==null?end.minusDays(30):from;if(end.isBefore(start)||start.isBefore(end.minusDays(370)))throw new BusinessException("OPS_METRIC_RANGE_INVALID","指标查询范围必须在 370 天以内");return reads.metrics(key,dimension,start,end);}
  public PageResult<SafetyAlertSummary> safetyAlerts(long id,String status,int page,int size){require(id,"safety:case:manage");return reads.safetyAlerts(status,page(page),size(size));}
  private void require(long id,String p){if(!admins.allowed(id,p))throw new BusinessException("ADMIN_FORBIDDEN","管理员权限不足");}
  private void requireAny(long id,String...ps){for(String p:ps)if(admins.allowed(id,p))return;throw new BusinessException("ADMIN_FORBIDDEN","管理员权限不足");}
  private int page(int p){return Math.max(1,p);}private int size(int s){return Math.min(100,Math.max(1,s));}
}
