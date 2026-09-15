package com.lingxi.identity.interfaces;

import com.lingxi.identity.api.CommonBlocker;
import com.lingxi.identity.api.CommunicationStyle;
import com.lingxi.identity.api.OnboardingProfile;
import com.lingxi.identity.api.OnboardingProfileResult;
import com.lingxi.identity.api.ProactivityLevel;
import com.lingxi.identity.application.OnboardingApplicationService;
import com.lingxi.kernel.ActorContextHolder;
import com.lingxi.kernel.ApiResponse;
import com.lingxi.kernel.RequestAttributes;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalTime;
import java.util.Set;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 新手引导基础画像接口（PRD 8.2 ONB-01）。
 *
 * <p>三个动作都必须可跳过：{@code PUT} 允许把画像整体写成空对象，{@code complete} 传 true
 * 即表示「我填完了」或「我跳过」。服务端不因此写入任何默认值——默认值只在读取时兜底，
 * 这样「用户明确选了温和」和「用户没选过」在数据上始终可区分。
 */
@RestController
@RequestMapping("/api/v1")
public class OnboardingController {
  private final OnboardingApplicationService onboardingService;

  public OnboardingController(OnboardingApplicationService onboardingService) {
    this.onboardingService = onboardingService;
  }

  /** 读取引导状态与已填画像；未完成引导时 {@code completed=false}。 */
  @GetMapping("/onboarding/profile")
  public ApiResponse<OnboardingProfileResult> getProfile(HttpServletRequest request) {
    return ok(onboardingService.getOnboardingProfile(userId()), request);
  }

  /**
   * 覆盖写入画像并可选结束引导。
   *
   * <p>不要求 {@code Idempotency-Key}：写入由账号乐观版本号保护，
   * 同一份画像重复提交结果相同，重放不会产生额外副作用。
   */
  @PutMapping("/onboarding/profile")
  public ApiResponse<OnboardingProfileResult> saveProfile(
      @RequestBody SaveProfileBody body, HttpServletRequest request) {
    OnboardingProfile profile =
        new OnboardingProfile(
            body.nickname(),
            body.sleepTime(),
            body.wakeTime(),
            body.weeklyAvailableMinutes(),
            body.remindWindowStart(),
            body.remindWindowEnd(),
            body.quietHoursStart(),
            body.quietHoursEnd(),
            body.communicationStyle(),
            body.proactivityLevel(),
            body.commonBlockers());
    return ok(
        onboardingService.saveOnboardingProfile(
            userId(), profile, body.complete(), body.expectedVersion()),
        request);
  }

  /** 重新进入引导（用户在设置里主动重做）；保留已填画像，只清完成标记。 */
  @PostMapping("/onboarding/reopen")
  public ApiResponse<OnboardingProfileResult> reopen(
      @RequestBody ReopenBody body, HttpServletRequest request) {
    return ok(onboardingService.reopenOnboarding(userId(), body.expectedVersion()), request);
  }

  private long userId() {
    return ActorContextHolder.requireUser().actorId();
  }

  private <T> ApiResponse<T> ok(T value, HttpServletRequest request) {
    return ApiResponse.success(
        value, String.valueOf(request.getAttribute(RequestAttributes.REQUEST_ID)));
  }

  /**
   * 引导画像更新请求体。
   *
   * <p>所有字段可选；{@code complete=true} 表示用户点了「完成」或「跳过」。
   * {@code expectedVersion} 必填，避免两个终端同时编辑画像时后写覆盖前写。
   */
  public record SaveProfileBody(
      String nickname,
      LocalTime sleepTime,
      LocalTime wakeTime,
      Integer weeklyAvailableMinutes,
      LocalTime remindWindowStart,
      LocalTime remindWindowEnd,
      LocalTime quietHoursStart,
      LocalTime quietHoursEnd,
      CommunicationStyle communicationStyle,
      ProactivityLevel proactivityLevel,
      Set<CommonBlocker> commonBlockers,
      boolean complete,
      long expectedVersion) {}

  public record ReopenBody(long expectedVersion) {}
}
