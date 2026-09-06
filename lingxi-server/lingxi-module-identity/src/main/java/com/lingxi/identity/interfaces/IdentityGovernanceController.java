package com.lingxi.identity.interfaces;

import com.lingxi.identity.api.AdminIdentityGovernanceFacade;
import com.lingxi.identity.api.AdminIdentityGovernanceFacade.*;
import com.lingxi.identity.api.AgeAppealFacade;
import com.lingxi.kernel.*;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import org.springframework.web.bind.annotation.*;

/** 用户治理后台接口与用户年龄申诉入口。 */
@RestController
public class IdentityGovernanceController {
  private final AdminIdentityGovernanceFacade admins;private final AgeAppealFacade appeals;
  public IdentityGovernanceController(AdminIdentityGovernanceFacade admins,AgeAppealFacade appeals){this.admins=admins;this.appeals=appeals;}
  @GetMapping("/admin-api/v1/identity-governance/users/{id}") public ApiResponse<?> detail(@PathVariable long id,HttpServletRequest r){return ok(admins.userDetail(actor().actorId(),id),r);}
  @GetMapping("/admin-api/v1/identity-governance/login-risks") public ApiResponse<?> risks(@RequestParam(required=false)String status,@RequestParam(required=false)String riskLevel,@RequestParam(defaultValue="1")int page,@RequestParam(defaultValue="20")int pageSize,HttpServletRequest r){return ok(admins.loginRisks(actor().actorId(),status,riskLevel,page,pageSize),r);}
  @PutMapping("/admin-api/v1/identity-governance/login-risks/{id}") public ApiResponse<?> resolveRisk(@PathVariable long id,@RequestBody RiskBody b,@RequestHeader("X-Operation-Reason")String reason,@RequestHeader("X-Ticket-No")String ticket,HttpServletRequest r){ActorContext a=actor();return ok(admins.resolveLoginRisk(new RiskResolutionCommand(a.actorId(),id,b.status(),b.resolution(),b.expectedVersion(),context(a,reason,ticket,r))),r);}
  @GetMapping("/admin-api/v1/identity-governance/age-appeals") public ApiResponse<?> ageAppeals(@RequestParam(required=false)String status,@RequestParam(defaultValue="1")int page,@RequestParam(defaultValue="20")int pageSize,HttpServletRequest r){return ok(admins.ageAppeals(actor().actorId(),status,page,pageSize),r);}
  @PutMapping("/admin-api/v1/identity-governance/age-appeals/{id}") public ApiResponse<?> reviewAppeal(@PathVariable long id,@RequestBody AppealReviewBody b,@RequestHeader("X-Operation-Reason")String reason,@RequestHeader("X-Ticket-No")String ticket,HttpServletRequest r){ActorContext a=actor();return ok(admins.reviewAgeAppeal(new AgeAppealReviewCommand(a.actorId(),id,b.approved(),b.resolution(),b.expectedVersion(),context(a,reason,ticket,r))),r);}
  @GetMapping("/admin-api/v1/identity-governance/privacy-requests") public ApiResponse<?> privacy(@RequestParam(required=false)String status,@RequestParam(defaultValue="1")int page,@RequestParam(defaultValue="20")int pageSize,HttpServletRequest r){return ok(admins.privacyRequests(actor().actorId(),status,page,pageSize),r);}
  @PostMapping("/api/v1/age-appeals") public ApiResponse<?> submit(@RequestBody AppealBody b,HttpServletRequest r){long user=ActorContextHolder.requireUser().actorId();return ok(appeals.submit(new AgeAppealFacade.SubmitAgeAppealCommand(user,b.claimedBirthDate(),b.evidenceRef())),r);}
  private ActorContext actor(){return ActorContextHolder.requireAdmin();}private OperationContext context(ActorContext a,String reason,String ticket,HttpServletRequest r){return new OperationContext(reason,ticket,String.valueOf(r.getAttribute(RequestAttributes.REQUEST_ID)),a.recentAuthentication());}private <T>ApiResponse<T>ok(T value,HttpServletRequest r){return ApiResponse.success(value,String.valueOf(r.getAttribute(RequestAttributes.REQUEST_ID)));}
  public record RiskBody(String status,String resolution,long expectedVersion){}public record AppealReviewBody(boolean approved,String resolution,long expectedVersion){}public record AppealBody(LocalDate claimedBirthDate,String evidenceRef){}
}
