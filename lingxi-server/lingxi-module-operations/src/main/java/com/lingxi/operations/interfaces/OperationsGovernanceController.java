package com.lingxi.operations.interfaces;

import com.lingxi.kernel.ActorContext;
import com.lingxi.kernel.ActorContextHolder;
import com.lingxi.kernel.ApiResponse;
import com.lingxi.kernel.RequestAttributes;
import com.lingxi.operations.api.AuditContext;
import com.lingxi.operations.api.OperationsGovernanceFacade;
import com.lingxi.operations.api.OperationsGovernanceFacade.*;
import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.springframework.web.bind.annotation.*;

/** 触达、客户端版本、合规、实验、功能开关、客服回复与指标事实管理接口。 */
@RestController
@RequestMapping("/admin-api/v1/governance")
public class OperationsGovernanceController {
  private final OperationsGovernanceFacade facade;
  public OperationsGovernanceController(OperationsGovernanceFacade facade){this.facade=facade;}

  @PostMapping("/campaigns")
  public ApiResponse<?> campaign(@RequestBody CampaignBody b,@RequestHeader("X-Operation-Reason")String reason,
      @RequestHeader("X-Ticket-No")String ticket,HttpServletRequest r){ActorContext a=actor();return ok(facade.saveCampaign(a.actorId(),new CampaignCommand(b.id(),b.campaignKey(),b.name(),b.channel(),b.audienceRule(),b.templateContent(),b.frequencyRule(),b.teenMarketingEnabled(),b.scheduledAt(),b.status(),b.expectedVersion(),context(a,reason,ticket,r))),r);}

  @PostMapping("/app-releases")
  public ApiResponse<?> appRelease(@RequestBody AppReleaseBody b,@RequestHeader("X-Operation-Reason")String reason,
      @RequestHeader("X-Ticket-No")String ticket,HttpServletRequest r){ActorContext a=actor();return ok(facade.saveAppRelease(a.actorId(),new AppReleaseCommand(b.id(),b.platform(),b.versionName(),b.versionCode(),b.minimumVersionCode(),b.forceUpgrade(),b.grayRule(),b.releaseNotes(),b.status(),b.expectedVersion(),context(a,reason,ticket,r))),r);}

  @PostMapping("/compliance-documents")
  public ApiResponse<?> compliance(@RequestBody ComplianceBody b,@RequestHeader("X-Operation-Reason")String reason,
      @RequestHeader("X-Ticket-No")String ticket,HttpServletRequest r){ActorContext a=actor();return ok(facade.saveComplianceDocument(a.actorId(),new ComplianceDocumentCommand(b.id(),b.documentType(),b.versionNo(),b.title(),b.contentRef(),b.contentDigest(),b.effectiveAt(),b.status(),b.expectedVersion(),context(a,reason,ticket,r))),r);}

  @PostMapping("/feature-flags")
  public ApiResponse<?> featureFlag(@RequestBody FeatureFlagBody b,@RequestHeader("X-Operation-Reason")String reason,
      @RequestHeader("X-Ticket-No")String ticket,HttpServletRequest r){ActorContext a=actor();return ok(facade.saveFeatureFlag(a.actorId(),new FeatureFlagCommand(b.id(),b.flagKey(),b.currentReleaseId(),b.mandatoryPolicy(),b.status(),b.expectedVersion(),context(a,reason,ticket,r))),r);}

  @PostMapping("/experiments")
  public ApiResponse<?> experiment(@RequestBody ExperimentBody b,@RequestHeader("X-Operation-Reason")String reason,
      @RequestHeader("X-Ticket-No")String ticket,HttpServletRequest r){ActorContext a=actor();return ok(facade.saveExperiment(a.actorId(),new ExperimentCommand(b.id(),b.experimentKey(),b.hypothesis(),b.audienceRule(),b.metricsJson(),b.currentReleaseId(),b.status(),b.expectedVersion(),context(a,reason,ticket,r))),r);}

  @DeleteMapping("/{resourceType}/{id}")
  public ApiResponse<?> retire(@PathVariable String resourceType,@PathVariable long id,@RequestParam long expectedVersion,
      @RequestHeader("X-Operation-Reason")String reason,@RequestHeader("X-Ticket-No")String ticket,HttpServletRequest r){ActorContext a=actor();return ok(facade.retire(a.actorId(),new RetireResourceCommand(resourceType,id,expectedVersion,context(a,reason,ticket,r))),r);}

  @PutMapping("/{resourceType}/{id}/runtime-status")
  public ApiResponse<?> runtimeStatus(@PathVariable String resourceType,@PathVariable long id,
      @RequestBody RuntimeStatusBody b,@RequestHeader("X-Operation-Reason")String reason,
      @RequestHeader("X-Ticket-No")String ticket,HttpServletRequest r){ActorContext a=actor();return ok(facade.changeRuntimeStatus(a.actorId(),new RuntimeStatusCommand(resourceType,id,b.releaseId(),b.targetStatus(),b.expectedVersion(),context(a,reason,ticket,r))),r);}

  @PostMapping("/support-tickets/{id}/messages")
  public ApiResponse<?> reply(@PathVariable long id,@RequestBody TicketReplyBody b,
      @RequestHeader("X-Operation-Reason")String reason,@RequestHeader("X-Ticket-No")String ticket,HttpServletRequest r){ActorContext a=actor();return ok(facade.replyTicket(a.actorId(),new TicketReplyCommand(id,b.content(),b.internalNote(),b.followUpAt(),context(a,reason,ticket,r))),r);}

  @PostMapping("/metrics")
  public ApiResponse<?> metric(@RequestBody MetricBody b,@RequestHeader("X-Operation-Reason")String reason,
      @RequestHeader("X-Ticket-No")String ticket,HttpServletRequest r){ActorContext a=actor();return ok(facade.recordMetric(a.actorId(),new MetricCommand(b.metricDate(),b.metricKey(),b.dimensionType(),b.dimensionValue(),b.metricValue(),b.sampleCount(),context(a,reason,ticket,r))),r);}

  private ActorContext actor(){return ActorContextHolder.requireAdmin();}
  private AuditContext context(ActorContext a,String reason,String ticket,HttpServletRequest r){return new AuditContext(reason,ticket,String.valueOf(r.getAttribute(RequestAttributes.REQUEST_ID)),a.recentAuthentication());}
  private <T>ApiResponse<T> ok(T value,HttpServletRequest r){return ApiResponse.success(value,String.valueOf(r.getAttribute(RequestAttributes.REQUEST_ID)));}

  public record CampaignBody(long id,String campaignKey,String name,String channel,String audienceRule,
      String templateContent,String frequencyRule,boolean teenMarketingEnabled,LocalDateTime scheduledAt,
      String status,long expectedVersion){}
  public record AppReleaseBody(long id,String platform,String versionName,long versionCode,long minimumVersionCode,
      boolean forceUpgrade,String grayRule,String releaseNotes,String status,long expectedVersion){}
  public record ComplianceBody(long id,String documentType,String versionNo,String title,String contentRef,
      String contentDigest,LocalDateTime effectiveAt,String status,long expectedVersion){}
  public record FeatureFlagBody(long id,String flagKey,Long currentReleaseId,boolean mandatoryPolicy,
      String status,long expectedVersion){}
  public record ExperimentBody(long id,String experimentKey,String hypothesis,String audienceRule,
      String metricsJson,Long currentReleaseId,String status,long expectedVersion){}
  public record TicketReplyBody(String content,boolean internalNote,LocalDateTime followUpAt){}
  public record MetricBody(LocalDate metricDate,String metricKey,String dimensionType,String dimensionValue,
      BigDecimal metricValue,long sampleCount){}
  public record RuntimeStatusBody(long releaseId,String targetStatus,long expectedVersion){}
}
