package com.lingxi.identity.api;

/** 手机号登录门面：首次使用自动建立账号，后续直接签发会话。 */
public interface PhoneLoginFacade {

  /**
   * 使用手机号登录；手机号尚未绑定账号时按出生日期建立新账号。
   *
   * @param command 手机号登录命令
   * @return 与断言登录一致的会话令牌
   */
  SessionTokens loginWithPhone(PhoneLoginCommand command);
}
