package com.lingxi.kernel;

import java.time.Instant;
import java.util.List;

/** 可恢复异步任务调度端口。 */
public interface AsyncJobScheduler {

  /**
   * 按任务类型和业务键幂等创建任务。
   *
   * @param jobType 任务类型
   * @param businessKey 业务唯一键
   * @param payloadJson 不含敏感明文的任务载荷
   * @param maxAttempts 最大执行次数
   * @return 已存在或新建的任务标识
   */
  long schedule(String jobType, String businessKey, String payloadJson, int maxAttempts);

  /** 在指定时刻之后调度任务，用于冷静期、延迟补偿等场景。 */
  long scheduleAt(
      String jobType, String businessKey, String payloadJson, int maxAttempts, Instant notBefore);

  /**
   * 把已存在但尚未执行完的任务推迟到指定时刻之后，并把状态恢复为待执行。
   *
   * <p>与 {@link #scheduleAt} 的区别很关键：后者对已存在的业务键是幂等空操作，
   * 无法把任务往后挪；而「因为免打扰时段不能打扰用户、等时段结束再发」这类场景
   * 恰恰需要把在途任务改期。任务已成功、已失败或已取消时不会被复活。
   *
   * @param jobType 任务类型
   * @param businessKey 业务唯一键
   * @param notBefore 推迟到该时刻之后才允许执行
   * @return 被推迟的任务标识；任务不存在或已终结时返回 null
   */
  Long defer(String jobType, String businessKey, Instant notBefore);

  /** 批量幂等创建任务；同一业务键已存在但载荷不同时整体失败。 */
  void scheduleBatch(List<AsyncJobRequest> requests);
}
