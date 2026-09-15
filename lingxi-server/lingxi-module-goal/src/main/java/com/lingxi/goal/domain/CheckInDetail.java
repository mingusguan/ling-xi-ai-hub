package com.lingxi.goal.domain;

import com.lingxi.goal.api.ActionDifficulty;
import com.lingxi.goal.api.CheckInResultType;
import com.lingxi.goal.api.EnergyLevel;
import com.lingxi.goal.api.MoodLevel;
import com.lingxi.kernel.BusinessException;

/**
 * 打卡记录的内容。
 *
 * <p>把「记录了什么」与「这条记录是否有效」分开：{@link CheckIn} 负责有效性与修正历史，
 * 本值对象负责用户填写的维度，修正时整体替换，不逐字段漂移。
 *
 * @param note 备注
 * @param evidenceReference 附件引用；文件本体由内容模块管理
 * @param actualMinutes 实际耗时（分钟）
 * @param perceivedDifficulty 主观难度；与计划中的行动难度使用同一把尺子，便于计算偏差
 * @param energyLevel 精力自评
 * @param moodLevel 情绪自评；仅用于趋势观察，不产出诊断结论
 * @param failureReason 失败原因；仅结果为「失败」时必填
 */
public record CheckInDetail(
    String note,
    String evidenceReference,
    Integer actualMinutes,
    ActionDifficulty perceivedDifficulty,
    EnergyLevel energyLevel,
    MoodLevel moodLevel,
    String failureReason) {
  /** 实际耗时上界：一天的总分钟数，超过该值视为填写错误。 */
  private static final int MAX_ACTUAL_MINUTES = 24 * 60;

  public CheckInDetail {
    note = trimToNull(note);
    evidenceReference = trimToNull(evidenceReference);
    failureReason = trimToNull(failureReason);
    if (actualMinutes != null && (actualMinutes <= 0 || actualMinutes > MAX_ACTUAL_MINUTES)) {
      throw new BusinessException("GOAL_INVALID_ACTUAL_MINUTES", "实际耗时超出合理范围");
    }
  }

  /** 只有备注与附件的最小内容，供离线同步等简化入口使用。 */
  public static CheckInDetail minimal(String note, String evidenceReference) {
    return new CheckInDetail(note, evidenceReference, null, null, null, null, null);
  }

  /**
   * 校验与打卡结果的一致性。
   *
   * <p>失败必须给出原因（复盘才有可用的输入）；非失败不允许带失败原因，
   * 否则同一条记录会出现两种互相矛盾的含义。
   */
  public void assertConsistentWith(CheckInResultType result) {
    if (result == CheckInResultType.FAILED) {
      if (failureReason == null) {
        throw new BusinessException("GOAL_FAILURE_REASON_REQUIRED", "记录失败必须填写失败原因");
      }
    } else if (failureReason != null) {
      throw new BusinessException("GOAL_UNEXPECTED_FAILURE_REASON", "只有结果为失败时才能填写失败原因");
    }
  }

  private static String trimToNull(String value) {
    if (value == null) {
      return null;
    }
    String trimmed = value.trim();
    return trimmed.isEmpty() ? null : trimmed;
  }
}
