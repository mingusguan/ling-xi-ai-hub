package com.lingxi.identity.api;

/** 身份模块对其他模块公开的账号与授权门面。 */
public interface IdentityFacade {

  /** 登记已经完成可信年龄验证的用户。 */
  RegisteredUserResult registerVerifiedUser(RegisterVerifiedUserCommand command);

  /** 获取其他模块执行服务端授权所需的最小视图。 */
  AccessProfile getAccessProfile(long userId);

  /** 有效监护关系建立后激活青少年账号。 */
  AccessProfile activateTeenAccount(ActivateTeenCommand command);

  /** 最后有效监护关系失效后限制青少年账号。 */
  AccessProfile restrictTeenAccount(RestrictTeenCommand command);

  /** 发起达到十八周岁后的显式成人迁移。 */
  AccessProfile beginAdultTransition(long userId);

  /** 用户确认成人条款后完成成人迁移。 */
  AccessProfile completeAdultTransition(long userId);

  /** 进入注销冷静期并使既有授权失效。 */
  AccessProfile beginClosing(long userId);

  /** 在冷静期内撤销注销并恢复进入注销前的账号状态。 */
  AccessProfile cancelClosing(long userId);

  /** 完成账号关闭。 */
  void completeClosing(long userId, long requestId);
}
