package com.lingxi.operations.interfaces;

import com.lingxi.kernel.*;
import com.lingxi.operations.api.OperationsAdminReadFacade;
import com.lingxi.operations.api.AuditContext;
import com.lingxi.operations.api.SafetyCaseAdminFacade;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin-api/v1")
public class OperationsAdminQueryController {
  private final OperationsAdminReadFacade facade;
  private final SafetyCaseAdminFacade safetyFacade;
  public OperationsAdminQueryController(OperationsAdminReadFacade facade,SafetyCaseAdminFacade safetyFacade){this.facade=facade;this.safetyFacade=safetyFacade;}
  @GetMapping("/dashboard") public ApiResponse<?> dashboard(HttpServletRequest r){return ok(facade.dashboard(admin()),r);}
  @GetMapping("/support-tickets") public ApiResponse<?> tickets(@RequestParam(required=false)String status,
      @RequestParam(required=false)String category,@RequestParam(defaultValue="1")int page,
      @RequestParam(defaultValue="20")int pageSize,HttpServletRequest r){return ok(facade.tickets(admin(),status,category,page,pageSize),r);}
  @GetMapping("/config-releases") public ApiResponse<?> releases(@RequestParam(required=false)String type,
      @RequestParam(required=false)String status,@RequestParam(defaultValue="1")int page,
      @RequestParam(defaultValue="20")int pageSize,HttpServletRequest r){return ok(facade.releases(admin(),type,status,page,pageSize),r);}
  @GetMapping("/audit-logs") public ApiResponse<?> audits(@RequestParam(required=false)String action,
      @RequestParam(required=false)Long operatorId,@RequestParam(defaultValue="1")int page,
      @RequestParam(defaultValue="20")int pageSize,HttpServletRequest r){return ok(facade.audits(admin(),action,operatorId,page,pageSize),r);}
  @GetMapping("/safety-cases") public ApiResponse<?> safety(@RequestParam(required=false)String status,
      @RequestParam(required=false)String riskLevel,@RequestParam(defaultValue="1")int page,
      @RequestParam(defaultValue="20")int pageSize,HttpServletRequest r){return ok(facade.safetyCases(admin(),status,riskLevel,page,pageSize),r);}
  @PutMapping("/safety-cases/{id}") public ApiResponse<?> transitionSafety(@PathVariable long id,
      @RequestBody SafetyBody body,@RequestHeader("X-Operation-Reason")String reason,
      @RequestHeader("X-Ticket-No")String ticket,HttpServletRequest r){ActorContext actor=ActorContextHolder.requireAdmin();return ok(safetyFacade.transition(new SafetyCaseAdminFacade.TransitionSafetyCaseCommand(actor.actorId(),id,body.status(),body.resolution(),body.expectedVersion(),new AuditContext(reason,ticket,String.valueOf(r.getAttribute(RequestAttributes.REQUEST_ID)),actor.recentAuthentication()))),r);}
  @GetMapping("/governance-resources") public ApiResponse<?> resources(@RequestParam String resourceType,
      @RequestParam(required=false)String status,@RequestParam(defaultValue="1")int page,
      @RequestParam(defaultValue="20")int pageSize,HttpServletRequest r){return ok(facade.resources(admin(),resourceType,status,page,pageSize),r);}
  @GetMapping("/support-tickets/{id}") public ApiResponse<?> ticket(@PathVariable long id,HttpServletRequest r){return ok(facade.ticketDetail(admin(),id),r);}
  @GetMapping("/analytics/metrics") public ApiResponse<?> metrics(@RequestParam(required=false)String metricKey,@RequestParam(required=false)String dimensionType,@RequestParam(required=false)java.time.LocalDateTime from,@RequestParam(required=false)java.time.LocalDateTime to,HttpServletRequest r){return ok(facade.metrics(admin(),metricKey,dimensionType,from,to),r);}
  @GetMapping("/safety-alerts") public ApiResponse<?> alerts(@RequestParam(required=false)String status,@RequestParam(defaultValue="1")int page,@RequestParam(defaultValue="20")int pageSize,HttpServletRequest r){return ok(facade.safetyAlerts(admin(),status,page,pageSize),r);}
  @PutMapping("/safety-alerts/{id}") public ApiResponse<?> transitionAlert(@PathVariable long id,
      @RequestBody SafetyAlertBody body,@RequestHeader("X-Operation-Reason")String reason,
      @RequestHeader("X-Ticket-No")String ticket,HttpServletRequest r){ActorContext actor=ActorContextHolder.requireAdmin();return ok(safetyFacade.transitionAlert(new SafetyCaseAdminFacade.TransitionSafetyAlertCommand(actor.actorId(),id,body.status(),body.expectedVersion(),new AuditContext(reason,ticket,String.valueOf(r.getAttribute(RequestAttributes.REQUEST_ID)),actor.recentAuthentication()))),r);}
  private long admin(){return ActorContextHolder.requireAdmin().actorId();}
  private <T>ApiResponse<T> ok(T v,HttpServletRequest r){return ApiResponse.success(v,String.valueOf(r.getAttribute(RequestAttributes.REQUEST_ID)));}
  public record SafetyBody(String status,String resolution,long expectedVersion){}
  public record SafetyAlertBody(String status,long expectedVersion){}
}
