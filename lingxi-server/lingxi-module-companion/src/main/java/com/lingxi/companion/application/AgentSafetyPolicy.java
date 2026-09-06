package com.lingxi.companion.application;

import java.util.*;
import org.springframework.stereotype.Component;

/** 最小本地危机预检；生产仍需叠加经评测的安全分类器。 */
@Component
public class AgentSafetyPolicy {
  private static final List<String> SELF_HARM_SIGNALS = List.of("自杀", "轻生", "不想活", "伤害自己", "结束生命");

  public Optional<SafetyDecision> evaluate(String input) {
    if (input != null && SELF_HARM_SIGNALS.stream().anyMatch(input::contains)) {
      return Optional.of(
          new SafetyDecision(
              "SELF_HARM",
              "CRITICAL",
              "MINIMUM_NECESSARY",
              "我很重视你现在的安全。请立即联系身边可信任的成年人或当地紧急服务，并尽量不要独处。如果你正处于立即危险中，请马上拨打当地急救电话。"));
    }
    return Optional.empty();
  }

  public record SafetyDecision(
      String riskType, String riskLevel, String disclosureScope, String safeResponse) {}
}
