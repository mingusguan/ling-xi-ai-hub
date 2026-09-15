package com.lingxi.goal.api;

import java.util.List;

/**
 * 入门目标模板（PRD 8.2 ONB-02 的模板入口）。
 *
 * <p>与运营模板的区别：入门模板由版本迁移随代码交付、面向所有用户只读，
 * 不需要后台审核；运营模板走 {@code content_goal_template} 的创建—提交—审核链路。
 *
 * @param templateKey 稳定业务键，客户端引用模板时使用
 * @param name 模板名称
 * @param summary 一句话说明
 * @param goalType 预置目标类型
 * @param defaultSuccessCriteria 预置完成标准；可为空
 * @param tags 分类标签
 * @param firstActions 首行动候选；至少一个，全部落在 5—30 分钟内
 */
public record StarterGoalTemplate(
    String templateKey,
    String name,
    String summary,
    GoalType goalType,
    String defaultSuccessCriteria,
    List<String> tags,
    List<StarterFirstAction> firstActions) {
  public StarterGoalTemplate {
    tags = tags == null ? List.of() : List.copyOf(tags);
    firstActions = firstActions == null ? List.of() : List.copyOf(firstActions);
  }
}
