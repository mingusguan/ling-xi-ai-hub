package com.lingxi.commerce.interfaces;

import com.lingxi.commerce.api.*;
import com.lingxi.kernel.*;
import jakarta.servlet.http.*;
import java.util.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
public class CommerceController {
  private final CommerceFacade commerce;
  private final EntitlementFacade entitlements;

  public CommerceController(CommerceFacade c, EntitlementFacade e) {
    commerce = c;
    entitlements = e;
  }

  @GetMapping("/products")
  public ApiResponse<List<ProductResult>> products(
      @RequestParam(defaultValue = "DEFAULT") String scene, HttpServletRequest r) {
    return ok(commerce.listProducts(user(), scene), r);
  }

  @PostMapping("/orders")
  public ApiResponse<OrderResult> order(
      @RequestHeader("Idempotency-Key") String key,
      @RequestBody OrderBody b,
      HttpServletRequest r) {
    return ok(
        commerce.createOrder(
            new CreateOrderCommand(
                key,
                user(),
                b.productId(),
                b.priceVersion(),
                b.channel(),
                b.guardianApprovalReference())),
        r);
  }

  @PostMapping("/payment-callbacks/{channel}")
  public ApiResponse<OrderResult> callback(
      @PathVariable String channel, @RequestBody String raw, HttpServletRequest r) {
    Map<String, String> h = new HashMap<>();
    r.getHeaderNames().asIterator().forEachRemaining(n -> h.put(n, r.getHeader(n)));
    return ok(commerce.handlePaymentCallback(new PaymentCallbackCommand(channel, raw, h)), r);
  }

  @PostMapping("/subscriptions/{id}/cancel")
  public ApiResponse<SubscriptionResult> cancel(
      @PathVariable long id,
      @RequestHeader("Idempotency-Key") String key,
      @RequestBody CancelBody b,
      HttpServletRequest r) {
    return ok(
        commerce.cancelSubscription(
            new CancelSubscriptionCommand(
                key,
                user(),
                id,
                b.cancelMode(),
                b.expectedVersion(),
                ActorContextHolder.requireUser().recentAuthentication())),
        r);
  }

  @GetMapping("/entitlements/{resource}")
  public ApiResponse<EntitlementResult> entitlement(
      @PathVariable String resource, HttpServletRequest r) {
    return ok(entitlements.get(user(), resource), r);
  }

  private long user() {
    return ActorContextHolder.requireUser().actorId();
  }

  private <T> ApiResponse<T> ok(T v, HttpServletRequest r) {
    return ApiResponse.success(v, String.valueOf(r.getAttribute(RequestAttributes.REQUEST_ID)));
  }

  public record OrderBody(
      long productId, int priceVersion, String channel, String guardianApprovalReference) {}

  public record CancelBody(String cancelMode, long expectedVersion) {}
}
