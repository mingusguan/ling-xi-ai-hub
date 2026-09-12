package com.lingxi.goal.interfaces;

import com.lingxi.goal.api.AchievementFacade;
import com.lingxi.goal.api.AchievementResult;
import com.lingxi.kernel.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

/** PC Web 与 HarmonyOS 共用的成就接口。 */
@RestController
@RequestMapping("/api/v1")
public class AchievementController {
  private final AchievementFacade achievementFacade;

  public AchievementController(AchievementFacade achievementFacade) {
    this.achievementFacade = achievementFacade;
  }

  @GetMapping("/achievements")
  public ApiResponse<PageResult<AchievementResult>> list(
      @RequestParam(required = false) Long goalId,
      @RequestParam(defaultValue = "1") int page,
      @RequestParam(defaultValue = "20") int pageSize,
      HttpServletRequest request) {
    return ApiResponse.success(
        achievementFacade.listAchievements(
            ActorContextHolder.requireUser().actorId(), goalId, page, pageSize),
        String.valueOf(request.getAttribute(RequestAttributes.REQUEST_ID)));
  }
}
