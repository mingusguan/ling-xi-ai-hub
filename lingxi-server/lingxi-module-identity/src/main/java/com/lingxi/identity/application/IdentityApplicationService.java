package com.lingxi.identity.application;

import com.lingxi.identity.api.AccessProfile;
import com.lingxi.identity.api.ActivateTeenCommand;
import com.lingxi.identity.api.AuthorizationChangedEvent;
import com.lingxi.identity.api.IdentityFacade;
import com.lingxi.identity.api.RegisterVerifiedUserCommand;
import com.lingxi.identity.api.RegisteredUserResult;
import com.lingxi.identity.api.RestrictTeenCommand;
import com.lingxi.identity.api.UserRegisteredEvent;
import com.lingxi.identity.domain.AgeAccessPolicy;
import com.lingxi.identity.domain.AgeAssessment;
import com.lingxi.identity.domain.AgeVerification;
import com.lingxi.identity.domain.IdentityRepository;
import com.lingxi.identity.domain.User;
import com.lingxi.kernel.BusinessException;
import com.lingxi.kernel.DomainEventPublisher;
import com.lingxi.kernel.IdGenerator;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.HexFormat;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;

/** 身份准入和账号状态应用服务。 */
@Service
public class IdentityApplicationService implements IdentityFacade {
  private final IdentityRepository repository;
  private final AgeAccessPolicy ageAccessPolicy;
  private final IdGenerator idGenerator;
  private final DomainEventPublisher eventPublisher;
  private final Clock clock;

  @Autowired
  public IdentityApplicationService(
      IdentityRepository repository,
      AgeAccessPolicy ageAccessPolicy,
      IdGenerator idGenerator,
      DomainEventPublisher eventPublisher) {
    this(repository, ageAccessPolicy, idGenerator, eventPublisher, Clock.systemUTC());
  }

  IdentityApplicationService(
      IdentityRepository repository,
      AgeAccessPolicy ageAccessPolicy,
      IdGenerator idGenerator,
      DomainEventPublisher eventPublisher,
      Clock clock) {
    this.repository = Objects.requireNonNull(repository);
    this.ageAccessPolicy = Objects.requireNonNull(ageAccessPolicy);
    this.idGenerator = Objects.requireNonNull(idGenerator);
    this.eventPublisher = Objects.requireNonNull(eventPublisher);
    this.clock = Objects.requireNonNull(clock);
  }

  @Override
  @Transactional
  public RegisteredUserResult registerVerifiedUser(RegisterVerifiedUserCommand command) {
    validate(command);
    String digest = registrationDigest(command);
    User existed = repository.findByRegistrationKey(command.requestKey()).orElse(null);
    if (existed != null) {
      if (!existed.getRegistrationDigest().equals(digest)) {
        throw new BusinessException("IDENTITY_IDEMPOTENCY_CONFLICT", "同一注册请求不能提交不同的身份资料");
      }
      return result(existed);
    }
    Instant instant = clock.instant();
    LocalDateTime now = utc(instant);
    AgeAssessment assessment =
        ageAccessPolicy.assess(
            command.verifiedBirthDate(), LocalDate.ofInstant(instant, ZoneOffset.UTC));
    long userId = idGenerator.nextId();
    User user =
        User.register(
            userId,
            idGenerator.nextPublicId(),
            command.requestKey(),
            digest,
            assessment,
            command.timezone(),
            now);
    AgeVerification verification =
        new AgeVerification(
            idGenerator.nextId(),
            userId,
            command.verificationMethod(),
            command.evidenceReference(),
            command.verifiedBirthDate(),
            assessment.ageBand(),
            now,
            now);
    repository.insertRegistration(user, verification);
    eventPublisher.publish(
        new UserRegisteredEvent(
            idGenerator.nextEventId(),
            userId,
            user.getPublicId(),
            user.getAgeBand(),
            user.getStatus(),
            user.getAuthorizationVersion(),
            instant));
    return result(user);
  }

  @Override
  @Transactional(readOnly = true)
  public AccessProfile getAccessProfile(long userId) {
    return access(requireUser(userId));
  }

  @Override
  @Transactional
  public AccessProfile activateTeenAccount(ActivateTeenCommand command) {
    User user = requireUser(command.teenUserId());
    long previous = user.getVersion();
    user.activateTeenAccount(utc(clock.instant()));
    persist(user, previous);
    publishAuthorization(user, "GUARDIAN_BOUND");
    return access(user);
  }

  @Override
  @Transactional
  public AccessProfile restrictTeenAccount(RestrictTeenCommand command) {
    User user = requireUser(command.teenUserId());
    long previous = user.getVersion();
    user.restrictForGuardianLoss(utc(clock.instant()));
    persist(user, previous);
    publishAuthorization(user, "GUARDIAN_LOST:" + command.guardianRelationId());
    return access(user);
  }

  @Override
  @Transactional
  public AccessProfile beginAdultTransition(long userId) {
    User user = requireUser(userId);
    long previous = user.getVersion();
    Instant now = clock.instant();
    user.beginAdultTransition(LocalDate.ofInstant(now, ZoneOffset.UTC), utc(now));
    persist(user, previous);
    publishAuthorization(user, "ADULT_TRANSITION_STARTED");
    return access(user);
  }

  @Override
  @Transactional
  public AccessProfile completeAdultTransition(long userId) {
    User user = requireUser(userId);
    long previous = user.getVersion();
    user.completeAdultTransition(utc(clock.instant()));
    persist(user, previous);
    publishAuthorization(user, "ADULT_TRANSITION_COMPLETED");
    return access(user);
  }

  @Override
  @Transactional
  public AccessProfile beginClosing(long userId) {
    User user = requireUser(userId);
    long previous = user.getVersion();
    user.beginClosing(utc(clock.instant()));
    persist(user, previous);
    publishAuthorization(user, "ACCOUNT_CLOSING");
    return access(user);
  }

  @Override
  @Transactional
  public AccessProfile cancelClosing(long userId) {
    User user = requireUser(userId);
    long previous = user.getVersion();
    user.cancelClosing(utc(clock.instant()));
    persist(user, previous);
    publishAuthorization(user, "ACCOUNT_CLOSING_CANCELLED");
    return access(user);
  }

  @Override
  @Transactional
  public void completeClosing(long userId, long requestId) {
    User user = repository.findById(userId).orElse(null);
    if (user == null && repository.isLogicallyDeleted(userId)) {
      return;
    }
    if (user == null) {
      throw new BusinessException("IDENTITY_USER_NOT_FOUND", "用户不存在");
    }
    long previous = user.getVersion();
    user.completeClosing(utc(clock.instant()));
    persist(user, previous);
    publishAuthorization(user, "ACCOUNT_CLOSED");
    repository.logicallyDeleteClosedUser(userId, requestId);
  }

  private User requireUser(long userId) {
    return repository
        .findById(userId)
        .orElseThrow(() -> new BusinessException("IDENTITY_USER_NOT_FOUND", "用户不存在"));
  }

  private void persist(User user, long previousVersion) {
    if (!repository.update(user, previousVersion)) {
      throw new BusinessException("IDENTITY_VERSION_CONFLICT", "账号已被其他请求更新");
    }
  }

  private void publishAuthorization(User user, String reason) {
    Instant now = clock.instant();
    eventPublisher.publish(
        new AuthorizationChangedEvent(
            idGenerator.nextEventId(),
            user.getId(),
            user.getStatus(),
            user.getAuthorizationVersion(),
            reason,
            user.getVersion(),
            now));
  }

  private AccessProfile access(User user) {
    return new AccessProfile(
        user.getId(),
        user.getAgeBand(),
        user.getStatus(),
        user.getAuthorizationVersion(),
        user.canUseCoreFeatures());
  }

  private void validate(RegisterVerifiedUserCommand command) {
    Objects.requireNonNull(command, "command");
    if (isBlank(command.requestKey())
        || isBlank(command.verificationMethod())
        || isBlank(command.evidenceReference())
        || command.verifiedBirthDate() == null
        || isBlank(command.timezone())) {
      throw new BusinessException("IDENTITY_INVALID_REGISTRATION", "注册资料不完整");
    }
    try {
      ZoneId.of(command.timezone());
    } catch (RuntimeException exception) {
      throw new BusinessException("IDENTITY_INVALID_TIMEZONE", "用户时区不合法");
    }
  }

  private String registrationDigest(RegisterVerifiedUserCommand command) {
    String source =
        command.verifiedBirthDate()
            + "|"
            + command.verificationMethod().trim()
            + "|"
            + command.evidenceReference().trim()
            + "|"
            + command.timezone().trim();
    try {
      return HexFormat.of()
          .formatHex(
              MessageDigest.getInstance("SHA-256").digest(source.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException("JVM 不支持 SHA-256", exception);
    }
  }

  private RegisteredUserResult result(User user) {
    return new RegisteredUserResult(
        user.getId(),
        user.getPublicId(),
        user.getAgeBand(),
        user.getStatus(),
        user.getAuthorizationVersion());
  }

  private LocalDateTime utc(Instant instant) {
    return LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
  }

  private boolean isBlank(String value) {
    return value == null || value.isBlank();
  }
}
