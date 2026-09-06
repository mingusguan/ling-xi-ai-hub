package com.lingxi.identity.application;

import com.lingxi.identity.api.RegisterVerifiedUserCommand;
import com.lingxi.identity.api.RegisterWithAssertionCommand;
import com.lingxi.identity.api.RegisteredUserResult;
import com.lingxi.identity.api.RegistrationFacade;
import com.lingxi.identity.api.VerifiedIdentityAssertion;
import com.lingxi.identity.domain.IdentitySecurityRepository;
import com.lingxi.identity.domain.LoginIdentity;
import com.lingxi.kernel.BusinessException;
import com.lingxi.kernel.IdGenerator;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 受信断言注册与登录身份落库事务。 */
@Service
public class RegistrationApplicationService implements RegistrationFacade {
  private final IdentityApplicationService identityService;
  private final IdentitySecurityRepository securityRepository;
  private final IdentityAssertionVerifier assertionVerifier;
  private final IdGenerator idGenerator;

  public RegistrationApplicationService(
      IdentityApplicationService identityService,
      IdentitySecurityRepository securityRepository,
      IdentityAssertionVerifier assertionVerifier,
      IdGenerator idGenerator) {
    this.identityService = identityService;
    this.securityRepository = securityRepository;
    this.assertionVerifier = assertionVerifier;
    this.idGenerator = idGenerator;
  }

  @Override
  @Transactional
  public RegisteredUserResult register(RegisterWithAssertionCommand command) {
    if (command == null || command.requestKey() == null || command.requestKey().isBlank()) {
      throw new BusinessException("IDENTITY_INVALID_REGISTRATION", "注册请求不完整");
    }
    VerifiedIdentityAssertion assertion = assertionVerifier.verify(command.signedAssertion(), true);
    RegisteredUserResult user =
        identityService.registerVerifiedUser(
            new RegisterVerifiedUserCommand(
                command.requestKey(),
                assertion.verifiedBirthDate(),
                assertion.verificationMethod(),
                assertion.evidenceReference(),
                command.timezone()));

    LoginIdentity existing =
        securityRepository
            .findLoginIdentity(assertion.channel(), assertion.subjectHash())
            .orElse(null);
    if (existing != null) {
      if (existing.userId() != user.userId()) {
        throw new BusinessException("IDENTITY_LOGIN_ALREADY_BOUND", "该登录身份已绑定其他账号");
      }
      return user;
    }
    LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
    securityRepository.insertLoginIdentity(
        new LoginIdentity(
            idGenerator.nextId(),
            user.userId(),
            assertion.channel(),
            assertion.subjectHash(),
            now,
            now));
    return user;
  }
}
