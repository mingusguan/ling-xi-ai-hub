package com.lingxi.goal.api;

/**
 * 行动实例打卡或修正命令。
 *
 * @param requestKey 幂等键
 * @param result 打卡结果；失败时必须在 {@code failureReason} 给出原因
 * @param note 备注
 * @param evidenceReference 附件引用；文件本体由内容模块管理
 * @param actualMinutes 实际耗时（分钟）
 * @param perceivedDifficulty 主观难度；与计划中的行动难度使用同一把尺子
 * @param energyLevel 精力自评
 * @param moodLevel 情绪自评；仅用于趋势观察，不产出诊断结论
 * @param failureReason 失败原因；仅结果为失败时允许填写
 * @param correction 是否为修正已有打卡；修正会保留原记录并写入新记录
 */
public record CheckInCommand(
    String requestKey,
    long userId,
    long occurrenceId,
    CheckInResultType result,
    String note,
    String evidenceReference,
    Integer actualMinutes,
    ActionDifficulty perceivedDifficulty,
    EnergyLevel energyLevel,
    MoodLevel moodLevel,
    String failureReason,
    boolean correction) {

  /** 只有结果与备注的最小命令，供离线同步等简化入口使用。 */
  public static CheckInCommand minimal(
      String requestKey,
      long userId,
      long occurrenceId,
      CheckInResultType result,
      String note,
      String evidenceReference,
      boolean correction) {
    return new CheckInCommand(
        requestKey, userId, occurrenceId, result, note, evidenceReference, null, null, null, null, null, correction);
  }
}
