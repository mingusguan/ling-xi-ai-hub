package com.lingxi.identity.application;

import com.lingxi.identity.api.*;
import com.lingxi.identity.api.AdminIdentityGovernanceFacade.*;
import com.lingxi.identity.domain.IdentityGovernanceRepository;
import com.lingxi.kernel.*;
import java.time.*;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 用户详情、登录风险、年龄申诉和隐私队列的治理用例。 */
@Service
public class IdentityGovernanceApplicationService implements AdminIdentityGovernanceFacade,AgeAppealFacade {
  private final IdentityGovernanceRepository repository;private final AdminAuthorizationFacade admins;
  private final AuthenticationFacade authentication;private final IdGenerator ids;private final DomainEventPublisher events;
  public IdentityGovernanceApplicationService(IdentityGovernanceRepository repository,AdminAuthorizationFacade admins,
      AuthenticationFacade authentication,IdGenerator ids,DomainEventPublisher events){this.repository=repository;
    this.admins=admins;this.authentication=authentication;this.ids=ids;this.events=events;}
  @Override @Transactional(readOnly=true) public UserDetail userDetail(long adminId,long userId){require(adminId,"identity:user:read");AdminUserFacade.AdminUserSummary user=repository.user(userId);if(user==null)throw new BusinessException("IDENTITY_USER_NOT_FOUND","用户不存在");return new UserDetail(user,repository.devices(userId),repository.ageVerifications(userId));}
  @Override @Transactional(readOnly=true) public PageResult<LoginRiskSummary> loginRisks(long adminId,String status,String level,int page,int size){require(adminId,"identity:risk:manage");return repository.loginRisks(status,level,page(page),size(size));}
  @Override @Transactional public LoginRiskSummary resolveLoginRisk(RiskResolutionCommand c){require(c.operatorAdminId(),"identity:risk:manage");context(c.context());if(!Set.of("REVIEWING","RESOLVED","DISMISSED").contains(c.status())||blank(c.resolution()))invalid("风险处置参数不合法");LoginRiskSummary r=repository.resolveLoginRisk(c.operatorAdminId(),c.id(),c.status(),c.resolution(),c.expectedVersion(),now());audit(c.operatorAdminId(),"LOGIN_RISK_RESOLVE","LOGIN_RISK",r.id(),r.version(),c.context());return r;}
  @Override @Transactional(readOnly=true) public PageResult<AgeAppealSummary> ageAppeals(long adminId,String status,int page,int size){require(adminId,"identity:age-appeal:manage");return repository.ageAppeals(status,page(page),size(size));}
  @Override @Transactional public AgeAppealSummary reviewAgeAppeal(AgeAppealReviewCommand c){require(c.operatorAdminId(),"identity:age-appeal:manage");context(c.context());if(blank(c.resolution()))invalid("审核结论不能为空");AgeAppealSummary current=repository.findAgeAppeal(c.id());if(current==null)throw new BusinessException("IDENTITY_AGE_APPEAL_NOT_FOUND","年龄申诉不存在");if(c.approved()){if(Period.between(current.claimedBirthDate(),LocalDate.now(ZoneOffset.UTC)).getYears()<14)throw new BusinessException("IDENTITY_MINIMUM_AGE_REQUIRED","灵犀伴行仅向 14 岁及以上用户开放");AdminUserFacade.AdminUserSummary user=repository.user(current.userId());if(user==null)throw new BusinessException("IDENTITY_USER_NOT_FOUND","用户不存在");if(Set.of("CLOSING","CLOSED").contains(user.status()))throw new BusinessException("IDENTITY_AGE_APPEAL_ACCOUNT_STATE_INVALID","注销中或已注销账号不能通过年龄申诉重新激活");}AgeAppealSummary result=repository.reviewAgeAppeal(c.operatorAdminId(),ids.nextId(),c.id(),c.approved(),c.resolution(),c.expectedVersion(),now());if(c.approved())authentication.revokeAllSessions(result.userId(),"AGE_APPEAL_APPROVED");audit(c.operatorAdminId(),"AGE_APPEAL_REVIEW","AGE_APPEAL",result.id(),result.version(),c.context());return result;}
  @Override @Transactional(readOnly=true) public PageResult<PrivacyRequestSummary> privacyRequests(long adminId,String status,int page,int size){require(adminId,"identity:user:read");return repository.privacyRequests(status,page(page),size(size));}
  @Override @Transactional public AgeAppealSummary submit(SubmitAgeAppealCommand c){if(c==null||c.userId()<=0||c.claimedBirthDate()==null||blank(c.evidenceRef()))invalid("年龄申诉参数不完整");if(Period.between(c.claimedBirthDate(),LocalDate.now(ZoneOffset.UTC)).getYears()<14)throw new BusinessException("IDENTITY_MINIMUM_AGE_REQUIRED","灵犀伴行仅向 14 岁及以上用户开放");if(repository.user(c.userId())==null)throw new BusinessException("IDENTITY_USER_NOT_FOUND","用户不存在");if(repository.hasPendingAgeAppeal(c.userId()))throw new BusinessException("IDENTITY_AGE_APPEAL_PENDING","已有待审核的年龄申诉");long id=ids.nextId();return repository.insertAgeAppeal(id,"AA"+id,c.userId(),c.claimedBirthDate(),c.evidenceRef(),now());}
  private void audit(long admin,String action,String type,long id,long version,OperationContext c){events.publish(new AdminActionAuditedEvent(UUID.randomUUID().toString(),admin,action,type,String.valueOf(id),version,c.reason(),c.ticketNo(),Instant.now()));}
  private void require(long admin,String permission){if(admin<=0||!admins.allowed(admin,permission))throw new BusinessException("ADMIN_FORBIDDEN","管理员权限不足");}private void context(OperationContext c){if(c==null||!c.recentAuthentication()||blank(c.reason())||blank(c.ticketNo()))throw new BusinessException("ADMIN_HIGH_RISK_CONTEXT_REQUIRED","操作需要近期认证、具体原因和工单号");}private boolean blank(String v){return v==null||v.isBlank();}private void invalid(String m){throw new BusinessException("IDENTITY_GOVERNANCE_COMMAND_INVALID",m);}private int page(int v){return Math.max(1,v);}private int size(int v){return Math.min(100,Math.max(1,v));}private LocalDateTime now(){return LocalDateTime.now(ZoneOffset.UTC);}
}
