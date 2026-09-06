package com.lingxi.identity.api;

/** 协议证据与隐私权利公开门面。 */
public interface PrivacyFacade {

  /** 追加协议证据，不覆盖历史记录。 */
  void recordConsent(RecordConsentCommand command);

  /** 幂等创建隐私请求和可恢复后台任务。 */
  PrivacyRequestResult createRequest(CreatePrivacyRequestCommand command);

  /** 查询本人隐私请求。 */
  PrivacyRequestResult getRequest(long userId, long requestId);

  /** 在冷静期任务执行前撤销本人账号注销请求。 */
  PrivacyRequestResult cancelAccountClosure(long userId, long requestId);

  /** 下载仍在有效期内的本人加密导出包。 */
  PrivacyExportResult downloadExport(long userId, long requestId);

  /** 人工工单完成后收口更正请求；仅供单体内部运营模块调用。 */
  void completeCorrection(long userId, long requestId, long ticketId);

  /** 人工解除交易阻断后恢复注销任务，由新幂等任务再次核验全部前置条件。 */
  void resumeAccountClosure(long userId, long requestId, long ticketId);
}
