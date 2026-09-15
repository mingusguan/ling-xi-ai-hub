package com.lingxi.goal.api;

/**
 * 打卡时记录的精力量表。
 *
 * <p>与结果分开记录：同为「完成」，精力耗尽和精力充沛对下一步计划的含义完全不同，
 * 复盘需要靠这个维度判断是计划过载还是执行方式问题。
 */
public enum EnergyLevel {
  /** 精力偏低。 */
  LOW,
  /** 一般，未填写时的默认档。 */
  NORMAL,
  /** 精力充沛。 */
  HIGH
}
