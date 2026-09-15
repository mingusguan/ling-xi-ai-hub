package com.lingxi.companion.application;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * 模型不可用时的降级约定。
 *
 * <p>这些常量既是产品文案也是可观测性契约：后台按 {@code UNAVAILABLE} / {@code DEGRADED}
 * 把降级回复与真实模型输出分开统计。如果哪天有人把文案改得含糊（例如「我没什么想说的」），
 * 用户会把系统故障理解成伙伴的敷衍，因此这里把语义固定住。
 */
class AgentRunDegradationTest {

  @Test
  void degradedReplyExplainsTheFailureInsteadOfPretendingToBeSilence() {
    String reply = AgentRunJobHandler.DEGRADED_REPLY;
    // 必须说明「这次没有生成回复」，而不是让用户以为伙伴无话可说。
    assertThat(reply).contains("没有生成有效回复");
    // 必须给出下一步动作，否则用户只能反复重试。
    assertThat(reply).contains("稍后");
    assertThat(reply).contains("客服工单");
    // 不得出现拟人化的情绪表达：这是系统状态说明，不是伙伴的态度。
    assertThat(reply).doesNotContain("不想说");
    assertThat(reply).doesNotContain("心情");
  }

  @Test
  void degradationIsMarkedSoItIsNotCountedAsModelOutput() {
    assertThat(AgentRunJobHandler.DEGRADED_MODEL_VERSION).isEqualTo("UNAVAILABLE");
    assertThat(AgentRunJobHandler.DEGRADED_PROMPT_VERSION).isEqualTo("DEGRADED");
  }
}
