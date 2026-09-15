package com.lingxi.companion.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.lingxi.companion.api.SafetyRiskDetectedEvent;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * 安全事件的读访问收口。
 *
 * <p>为什么需要这条守卫：安全事件来自风险分级判定，属于最敏感的一类数据。它的读访问
 * 必须只有两条合法路径——用户自己的隐私导出（按 userId 过滤）与运营侧的风险投影
 * （由 {@code SafetyRiskDetectedEvent} 这一最小披露事件驱动）。
 *
 * <p>而 {@code AgentRepository} 是对话模块内的数据入口。一旦这里出现「按风险等级列出
 * 安全事件」之类的读方法，调用方很容易顺手把它接进用户可见的接口，从而把某个用户的
 * 风险记录暴露给别人——这种改动在评审里看起来往往只是「加一个查询方法」，很难靠人眼拦住。
 * 因此把「仓储只写不读」固定成回归测试。
 */
class SafetyEventAccessTest {

  @Test
  void agentRepositoryExposesNoWayToReadSafetyEvents() {
    List<String> safetyMethods =
        Arrays.stream(AgentRepository.class.getDeclaredMethods())
            .filter(method -> !Modifier.isStatic(method.getModifiers()))
            .filter(
                method ->
                    method.getName().toLowerCase().contains("safety")
                        || method.getReturnType().getSimpleName().contains("Safety"))
            .map(Method::getName)
            .toList();

    // 只允许写入；出现任何其它 safety* 方法都说明读访问被打开了。
    assertThat(safetyMethods)
        .as("AgentRepository 只应提供 insertSafetyEvent，读访问必须走隐私导出或运营投影")
        .containsExactly("insertSafetyEvent");
  }

  /**
   * 对外事件必须保持最小披露。
   *
   * <p>{@code SafetyRiskDetectedEvent} 是唯一对外暴露的安全事实，它只能带标识与分级，
   * 不能带用户原话——风险判定往往正是由对话内容触发的，一旦把原文放进事件，
   * 它会随事件流被投递到运营投影、审计与导出等多条链路，等于把最敏感的文本铺开。
   * 因此这里把「事件字段里不许出现内容类字段」固定下来。
   */
  @Test
  void safetyEventCarriesNoUserContent() {
    List<String> components =
        Arrays.stream(SafetyRiskDetectedEvent.class.getRecordComponents())
            .map(java.lang.reflect.RecordComponent::getName)
            .toList();

    assertThat(components)
        .as("安全事件只应带标识与分级，不得携带用户原文或摘要以保持最小披露")
        .noneMatch(
            name ->
                name.toLowerCase().contains("content")
                    || name.toLowerCase().contains("text")
                    || name.toLowerCase().contains("message")
                    || name.toLowerCase().contains("input")
                    || name.toLowerCase().contains("excerpt")
                    || name.toLowerCase().contains("summary"));
  }
}
