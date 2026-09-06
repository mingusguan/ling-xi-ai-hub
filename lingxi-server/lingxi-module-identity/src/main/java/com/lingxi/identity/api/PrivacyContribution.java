package com.lingxi.identity.api;

/** 单个业务模块的隐私处理结果。 */
public record PrivacyContribution(String exportJson, int affectedRows, String pendingReference) {

  public static PrivacyContribution exported(String exportJson) {
    return new PrivacyContribution(exportJson, 0, null);
  }

  public static PrivacyContribution deleted(int affectedRows) {
    return new PrivacyContribution(null, Math.max(affectedRows, 0), null);
  }

  /** 返回需要人工处理的业务引用，隐私请求不能提前完成。 */
  public static PrivacyContribution pending(String reference) {
    return new PrivacyContribution(null, 0, reference);
  }

  public static PrivacyContribution unchanged() {
    return new PrivacyContribution(null, 0, null);
  }
}
