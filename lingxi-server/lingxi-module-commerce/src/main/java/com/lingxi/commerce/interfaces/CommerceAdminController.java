package com.lingxi.commerce.interfaces;

import com.lingxi.commerce.api.AdminCommerceFacade;
import com.lingxi.kernel.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin-api/v1/commerce")
public class CommerceAdminController {
  private final AdminCommerceFacade facade;
  public CommerceAdminController(AdminCommerceFacade facade) { this.facade = facade; }
  @GetMapping("/overview") public ApiResponse<?> overview(HttpServletRequest r) { return ok(facade.overview(admin()), r); }
  @GetMapping("/products") public ApiResponse<?> products(HttpServletRequest r) { return ok(facade.products(admin()), r); }
  @GetMapping("/orders") public ApiResponse<?> orders(@RequestParam(required=false) String keyword,
      @RequestParam(required=false) String status, @RequestParam(defaultValue="1") int page,
      @RequestParam(defaultValue="20") int pageSize, HttpServletRequest r) {
    return ok(facade.orders(admin(), keyword, status, page, pageSize), r);
  }
  @GetMapping("/subscriptions") public ApiResponse<?> subscriptions(@RequestParam(required=false) String status,
      @RequestParam(defaultValue="1") int page, @RequestParam(defaultValue="20") int pageSize,
      HttpServletRequest r) { return ok(facade.subscriptions(admin(), status, page, pageSize), r); }
  @GetMapping("/entitlements") public ApiResponse<?> entitlements(@RequestParam(required=false) Long userId,
      @RequestParam(defaultValue="1") int page, @RequestParam(defaultValue="20") int pageSize,
      HttpServletRequest r) { return ok(facade.entitlements(admin(), userId, page, pageSize), r); }
  private long admin() { return ActorContextHolder.requireAdmin().actorId(); }
  private <T> ApiResponse<T> ok(T value, HttpServletRequest request) {
    return ApiResponse.success(value, String.valueOf(request.getAttribute(RequestAttributes.REQUEST_ID)));
  }
}
