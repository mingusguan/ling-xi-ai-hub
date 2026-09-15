package com.lingxi.goal.api;

/**
 * 今日工作台的规则化提示。
 *
 * <p>本字段在接入真实模型之前由确定性规则产生，不冒充模型输出：{@code source} 明确标注
 * 来源，客户端据此展示「规则提示」而不是「AI 建议」。模型接入后由同一结构承载模型结论，
 * 但必须同时给出依据，因此结构里保留 {@code basis}。
 *
 * @param type 提示类型：OVERDUE_BACKLOG 逾期积压、TODAY_OVERLOAD 今日过量、FIRST_STEP 先做哪一步、LONG_PENDING 长期未动
 * @param source 产生来源：RULE 规则、MODEL 模型（当前固定为 RULE）
 * @param title 一句话结论
 * @param basis 判断依据，必须能对应到用户可核对的数据
 * @param relatedOccurrenceId 关联的行动实例；无关联时为空
 */
public record TodaySuggestion(
    String type, String source, String title, String basis, Long relatedOccurrenceId) {

  /** 规则来源的提示。 */
  public static TodaySuggestion rule(
      String type, String title, String basis, Long relatedOccurrenceId) {
    return new TodaySuggestion(type, "RULE", title, basis, relatedOccurrenceId);
  }
}
