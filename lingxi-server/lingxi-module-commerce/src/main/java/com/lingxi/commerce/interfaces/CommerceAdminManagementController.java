package com.lingxi.commerce.interfaces;

import com.lingxi.commerce.api.AdminCommerceManagementFacade;
import com.lingxi.commerce.api.AdminCommerceManagementFacade.*;
import com.lingxi.kernel.*;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.time.LocalDateTime;
import org.springframework.web.bind.annotation.*;

/** 商品、价格、促销、退款、权益和对账后台接口。 */
@RestController
@RequestMapping("/admin-api/v1/commerce/manage")
public class CommerceAdminManagementController {
  private final AdminCommerceManagementFacade facade;
  public CommerceAdminManagementController(AdminCommerceManagementFacade facade){this.facade=facade;}
  @PostMapping("/products") public ApiResponse<?> product(@RequestBody ProductBody b,@RequestHeader("X-Operation-Reason")String reason,@RequestHeader("X-Ticket-No")String ticket,HttpServletRequest r){ActorContext a=actor();return ok(facade.saveProduct(a.actorId(),new ProductCommand(b.id(),b.productKey(),b.name(),b.scene(),b.billingPeriod(),b.agePolicy(),b.entitlementKey(),b.entitlementAmount(),b.status(),b.expectedVersion(),context(a,reason,ticket,r))),r);}
  @PostMapping("/prices") public ApiResponse<?> price(@RequestBody PriceBody b,@RequestHeader("X-Operation-Reason")String reason,@RequestHeader("X-Ticket-No")String ticket,HttpServletRequest r){ActorContext a=actor();return ok(facade.savePrice(a.actorId(),new PriceCommand(b.id(),b.productId(),b.versionNo(),b.amountMinor(),b.currency(),b.status(),b.expectedVersion(),context(a,reason,ticket,r))),r);}
  @PostMapping("/promotions") public ApiResponse<?> promotion(@RequestBody PromotionBody b,@RequestHeader("X-Operation-Reason")String reason,@RequestHeader("X-Ticket-No")String ticket,HttpServletRequest r){ActorContext a=actor();return ok(facade.savePromotion(a.actorId(),new PromotionCommand(b.id(),b.promotionKey(),b.name(),b.promotionType(),b.ruleJson(),b.audienceRule(),b.startsAt(),b.endsAt(),b.status(),b.expectedVersion(),context(a,reason,ticket,r))),r);}
  @GetMapping("/promotions") public ApiResponse<?> promotions(@RequestParam(required=false)String status,@RequestParam(defaultValue="1")int page,@RequestParam(defaultValue="20")int pageSize,HttpServletRequest r){return ok(facade.promotions(actor().actorId(),status,page,pageSize),r);}
  @PostMapping("/refunds/confirm") public ApiResponse<?> refund(@RequestBody RefundBody b,@RequestHeader("X-Operation-Reason")String reason,@RequestHeader("X-Ticket-No")String ticket,HttpServletRequest r){ActorContext a=actor();return ok(facade.confirmRefund(a.actorId(),new RefundCommand(b.channel(),b.refundTransactionId(),b.orderNo(),b.amountMinor(),b.refundReason(),context(a,reason,ticket,r))),r);}
  @PostMapping("/entitlements/adjust") public ApiResponse<?> entitlement(@RequestBody EntitlementBody b,@RequestHeader("X-Operation-Reason")String reason,@RequestHeader("X-Ticket-No")String ticket,HttpServletRequest r){ActorContext a=actor();return ok(facade.adjustEntitlement(a.actorId(),new EntitlementAdjustmentCommand(b.userId(),b.resourceKey(),b.delta(),b.expiresAt(),b.commandId(),context(a,reason,ticket,r))),r);}
  @GetMapping("/reconciliation-cases") public ApiResponse<?> reconciliation(@RequestParam(required=false)String status,@RequestParam(defaultValue="1")int page,@RequestParam(defaultValue="20")int pageSize,HttpServletRequest r){return ok(facade.reconciliationCases(actor().actorId(),status,page,pageSize),r);}
  @PutMapping("/reconciliation-cases/{id}") public ApiResponse<?> resolve(@PathVariable long id,@RequestBody ReconcileBody b,@RequestHeader("X-Operation-Reason")String reason,@RequestHeader("X-Ticket-No")String ticket,HttpServletRequest r){ActorContext a=actor();return ok(facade.resolveReconciliation(a.actorId(),new ReconciliationCommand(id,b.resolution(),b.expectedVersion(),context(a,reason,ticket,r))),r);}
  private ActorContext actor(){return ActorContextHolder.requireAdmin();}private OperationContext context(ActorContext a,String reason,String ticket,HttpServletRequest r){return new OperationContext(reason,ticket,String.valueOf(r.getAttribute(RequestAttributes.REQUEST_ID)),a.recentAuthentication());}private <T>ApiResponse<T>ok(T value,HttpServletRequest r){return ApiResponse.success(value,String.valueOf(r.getAttribute(RequestAttributes.REQUEST_ID)));}
  public record ProductBody(long id,String productKey,String name,String scene,String billingPeriod,String agePolicy,String entitlementKey,long entitlementAmount,String status,long expectedVersion){}
  public record PriceBody(long id,long productId,int versionNo,long amountMinor,String currency,String status,long expectedVersion){}
  public record PromotionBody(long id,String promotionKey,String name,String promotionType,String ruleJson,String audienceRule,LocalDateTime startsAt,LocalDateTime endsAt,String status,long expectedVersion){}
  public record RefundBody(String channel,String refundTransactionId,String orderNo,long amountMinor,String refundReason){}
  public record EntitlementBody(long userId,String resourceKey,long delta,Instant expiresAt,String commandId){}
  public record ReconcileBody(String resolution,long expectedVersion){}
}
