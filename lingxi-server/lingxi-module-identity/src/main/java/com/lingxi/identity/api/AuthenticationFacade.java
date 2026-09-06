package com.lingxi.identity.api;

/** 身份认证与会话公开门面。 */
public interface AuthenticationFacade {

  /** 使用签名断言登录。 */
  SessionTokens login(LoginWithAssertionCommand command);

  /** 轮换刷新令牌。 */
  SessionTokens refresh(RefreshSessionCommand command);

  /** 校验 Access Token 和当前授权版本。 */
  AuthenticatedSession authenticate(String accessToken);

  /** 撤销用户的全部会话。 */
  void revokeAllSessions(long userId, String reason);
}
