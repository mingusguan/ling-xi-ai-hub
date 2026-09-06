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

  /** 批量幂等创建任务；同一业务键已存在但载荷不同时整体失败。 */
  void scheduleBatch(List<AsyncJobRequest> requests);
}
