package com.lingxi.identity.application;

import com.lingxi.identity.api.VerifiedIdentityAssertion;

/** 外部身份渠道签名断言验证端口。 */
public interface IdentityAssertionVerifier {

  /**
   * 验证断言签名、时效和必要字段。
   *
   * @param signedAssertion 签名断言
   * @param birthDateRequired 是否要求年龄验证结果
   */
  VerifiedIdentityAssertion verify(String signedAssertion, boolean birthDateRequired);
}
