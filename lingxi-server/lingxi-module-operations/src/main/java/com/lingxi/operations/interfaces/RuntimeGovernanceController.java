package com.lingxi.operations.interfaces;

import com.lingxi.kernel.ApiResponse;
import com.lingxi.kernel.RequestAttributes;
import com.lingxi.operations.api.RuntimeGovernanceFacade;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** PC Web 与 HarmonyOS 使用同一 bootstrap 契约，保证功能和安全策略一致。 */
@RestController
@RequestMapping("/api/v1/runtime")
public class RuntimeGovernanceController {
  private final RuntimeGovernanceFacade facade;

  public RuntimeGovernanceController(RuntimeGovernanceFacade facade) {
    this.facade = facade;
  }

  @GetMapping("/bootstrap")
  public ApiResponse<?> bootstrap(
      @RequestParam String platform,
      @RequestParam(defaultValue = "0") long currentVersionCode,
      HttpServletRequest request) {
    return ApiResponse.success(
        facade.clientBootstrap(platform, currentVersionCode),
        String.valueOf(request.getAttribute(RequestAttributes.REQUEST_ID)));
  }
}
