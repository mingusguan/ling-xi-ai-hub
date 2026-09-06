package com.lingxi.relationship.interfaces;

import com.lingxi.kernel.*;import com.lingxi.relationship.api.GuardianDisputeFacade;import com.lingxi.relationship.api.GuardianDisputeFacade.*;import jakarta.servlet.http.HttpServletRequest;import org.springframework.web.bind.annotation.*;

/** 监护争议用户入口和后台处置接口。 */
@RestController
public class GuardianDisputeController {
  private final GuardianDisputeFacade facade;public GuardianDisputeController(GuardianDisputeFacade facade){this.facade=facade;}
  @PostMapping("/api/v1/guardian-disputes") public ApiResponse<?> submit(@RequestBody SubmitBody b,HttpServletRequest r){return ok(facade.submit(ActorContextHolder.requireUser().actorId(),new SubmitDisputeCommand(b.relationId(),b.reason())),r);}
  @GetMapping("/admin-api/v1/guardian-disputes") public ApiResponse<?> list(@RequestParam(required=false)String status,@RequestParam(defaultValue="1")int page,@RequestParam(defaultValue="20")int pageSize,HttpServletRequest r){return ok(facade.list(ActorContextHolder.requireAdmin().actorId(),status,page,pageSize),r);}
  @PutMapping("/admin-api/v1/guardian-disputes/{id}") public ApiResponse<?> resolve(@PathVariable long id,@RequestBody ResolveBody b,@RequestHeader("X-Operation-Reason")String reason,@RequestHeader("X-Ticket-No")String ticket,HttpServletRequest r){ActorContext a=ActorContextHolder.requireAdmin();return ok(facade.resolve(new ResolveDisputeCommand(a.actorId(),id,b.status(),b.resolution(),b.expectedVersion(),new OperationContext(reason,ticket,String.valueOf(r.getAttribute(RequestAttributes.REQUEST_ID)),a.recentAuthentication()))),r);}
  private <T>ApiResponse<T>ok(T v,HttpServletRequest r){return ApiResponse.success(v,String.valueOf(r.getAttribute(RequestAttributes.REQUEST_ID)));}public record SubmitBody(long relationId,String reason){}public record ResolveBody(String status,String resolution,long expectedVersion){}
}
