package com.lingxi.kernel;

/** 异步任务处理器。实现必须按 businessKey 保持业务幂等。 */
public interface AsyncJobHandler {

  /** 判断是否处理指定任务类型。 */
  boolean supports(String jobType);

  /**
   * 执行任务。
   *
   * @return 可安全持久化的结果 JSON
   */
  String handle(AsyncJobMessage message) throws Exception;
}
