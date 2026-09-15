package com.lingxi.goal.api;

/**
 * 行动重复类型。
 *
 * <p>取值与 PRD「按天、周、工作日、自定义星期、间隔重复」五种形式对齐：
 * 按天对应 {@link #DAILY}，周与自定义星期都由 {@link #WEEKLY} 携带具体星期集合，
 * 工作日由 {@link #WEEKDAYS} 表达，间隔重复由 {@link #INTERVAL} 配合间隔天数表达。
 */
public enum RecurrenceType {
  /** 单次行动，只在开始日期发生。 */
  ONCE,
  /** 每天发生。 */
  DAILY,
  /** 每周指定星期发生，必须有非空星期集合。 */
  WEEKLY,
  /** 工作日发生，即周一至周五。 */
  WEEKDAYS,
  /** 每隔固定天数发生一次，必须给定间隔天数。 */
  INTERVAL
}
