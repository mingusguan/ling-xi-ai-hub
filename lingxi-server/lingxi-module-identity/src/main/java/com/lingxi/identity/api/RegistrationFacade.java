package com.lingxi.identity.api;

/** 对客户端公开的安全注册门面。 */
public interface RegistrationFacade {

  /** 验证签名断言并原子建立账号和登录身份。 */
  RegisteredUserResult register(RegisterWithAssertionCommand command);
}
