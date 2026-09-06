package com.lingxi.identity.api;

/** 各业务模块参与隐私导出、删除与注销的数据处理端口。 实现必须幂等，失败时抛出异常，由数据库任务恢复重试。 */
public interface PrivacyDataContributor {
  /** 模块稳定名称，用于进度和审计。 */
  String moduleName();

  /** 处理当前模块拥有的数据，返回不含敏感明文的结果引用。 */
  PrivacyContribution process(PrivacyProcessingContext context);
}
