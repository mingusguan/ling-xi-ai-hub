package com.lingxi.identity.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lingxi.identity.api.*;
import com.lingxi.identity.domain.IdentitySecurityRepository;
import com.lingxi.identity.domain.PrivacyExportStore;
import com.lingxi.identity.domain.PrivacyRequest;
import com.lingxi.kernel.AsyncJobScheduler;
import com.lingxi.kernel.BusinessException;
import com.lingxi.kernel.IdGenerator;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.*;
import java.util.HexFormat;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 隐私权利请求的创建、查询与可恢复任务编排入口。 */
@Service
public class PrivacyApplicationService implements PrivacyFacade {
  public static final String JOB_TYPE = "identity.privacy-request";
  private static final Duration CLOSE_COOLING_PERIOD = Duration.ofDays(7);

  private final IdentitySecurityRepository repository;
  private final IdentityFacade identityFacade;
  private final AuthenticationFacade authenticationFacade;
  private final AsyncJobScheduler jobScheduler;
  private final IdGenerator idGenerator;
  private final ObjectMapper objectMapper;
  private final Clock clock;
  private final PrivacyExportStore exportStore;
  private final PrivacyScopeParser scopeParser;

  public PrivacyApplicationService(
      IdentitySecurityRepository repository,
      IdentityFacade identityFacade,
      AuthenticationFacade authenticationFacade,
      AsyncJobScheduler jobScheduler,
      IdGenerator idGenerator,
      ObjectMapper objectMapper,
      PrivacyExportStore exportStore,
      PrivacyScopeParser scopeParser) {
    this(
        repository,
        identityFacade,
        authenticationFacade,
        jobScheduler,
        idGenerator,
        objectMapper,
        exportStore,
        scopeParser,
        Clock.systemUTC());
  }

  PrivacyApplicationService(
      IdentitySecurityRepository repository,
      IdentityFacade identityFacade,
      AuthenticationFacade authenticationFacade,
      AsyncJobScheduler jobScheduler,
      IdGenerator idGenerator,
      ObjectMapper objectMapper,
      PrivacyExportStore exportStore,
      PrivacyScopeParser scopeParser,
      Clock clock) {
    this.repository = repository;
    this.identityFacade = identityFacade;
    this.authenticationFacade = authenticationFacade;
    this.jobScheduler = jobScheduler;
    this.idGenerator = idGenerator;
    this.objectMapper = objectMapper;
    this.exportStore = exportStore;
    this.scopeParser = scopeParser;
    this.clock = clock;
  }

  @Override
  @Transactional
  public void recordConsent(RecordConsentCommand command) {
    if (command == null
        || command.userId() <= 0
        || command.purpose() == null
        || blank(command.documentVersion())
        || blank(command.evidenceReference())) {
      throw new BusinessException("PRIVACY_INVALID_CONSENT", "协议证据参数不完整");
    }
    repository.insertConsent(
        idGenerator.nextId(),
        command.userId(),
        command.purpose(),
        command.documentVersion(),
        command.granted(),
        command.evidenceReference(),
        utc(clock.instant()));
  }

  @Override
  @Transactional
  public PrivacyRequestResult createRequest(CreatePrivacyRequestCommand command) {
    validate(command);
    if (!command.recentAuthentication()) {
      throw new BusinessException("AUTH_RECENT_AUTHENTICATION_REQUIRED", "隐私权利请求需要近期认证");
    }
    PrivacyScope scope = scopeParser.parse(command.scopeJson(), command.type());
    String normalizedScope = scopeParser.canonicalJson(scope);
    String digest = digest(command.userId() + "|" + command.type() + "|" + normalizedScope);
    PrivacyRequest existed = repository.findPrivacyRequestByKey(command.requestKey()).orElse(null);
    if (existed != null) {
      if (existed.getUserId() != command.userId() || !existed.getRequestDigest().equals(digest)) {
        throw new BusinessException("PRIVACY_IDEMPOTENCY_CONFLICT", "同一请求键不能提交不同隐私请求");
      }
      return result(existed);
    }
    Instant now = clock.instant();
    PrivacyRequest request =
        PrivacyRequest.create(
            idGenerator.nextId(),
            command.requestKey(),
            digest,
            command.userId(),
            command.type(),
            normalizedScope,
            utc(now));
    repository.insertPrivacyRequest(request);
    if (command.type() == PrivacyRequestType.CLOSE_ACCOUNT) {
      identityFacade.beginClosing(command.userId());
      authenticationFacade.revokeAllSessions(command.userId(), "ACCOUNT_CLOSING");
    }
    Instant notBefore =
        command.type() == PrivacyRequestType.CLOSE_ACCOUNT ? now.plus(CLOSE_COOLING_PERIOD) : now;
    jobScheduler.scheduleAt(
        JOB_TYPE, Long.toString(request.getId()), payload(request.getId()), 20, notBefore);
    return result(request);
  }

  @Override
  @Transactional(readOnly = true)
  public PrivacyRequestResult getRequest(long userId, long requestId) {
    PrivacyRequest request =
        repository
            .findPrivacyRequest(requestId)
            .orElseThrow(() -> new BusinessException("PRIVACY_REQUEST_NOT_FOUND", "隐私请求不存在"));
    if (request.getUserId() != userId) {
      throw new BusinessException("PRIVACY_REQUEST_FORBIDDEN", "无权查看该隐私请求");
    }
    return result(request);
  }

  @Override
  @Transactional
  public PrivacyRequestResult cancelAccountClosure(long userId, long requestId) {
    PrivacyRequest request =
        repository
            .findPrivacyRequest(requestId)
            .orElseThrow(
                () -> new BusinessException("PRIVACY_REQUEST_NOT_FOUND", "隐私请求不存在"));
    if (request.getUserId() != userId) {
      throw new BusinessException("PRIVACY_REQUEST_FORBIDDEN", "无权撤销该注销请求");
    }
    long previousVersion = request.getVersion();
    request.cancel(utc(clock.instant()));
    if (!repository.updatePrivacyRequest(request, previousVersion)) {
      throw new BusinessException("PRIVACY_REQUEST_CONFLICT", "隐私请求正在被处理");
    }
    identityFacade.cancelClosing(userId);
    return result(request);
  }

  @Override
  @Transactional(readOnly = true)
  public PrivacyExportResult downloadExport(long userId, long requestId) {
    PrivacyRequest request =
        repository
            .findPrivacyRequest(requestId)
            .orElseThrow(() -> new BusinessException("PRIVACY_REQUEST_NOT_FOUND", "隐私请求不存在"));
    if (request.getUserId() != userId
        || request.getType() != PrivacyRequestType.EXPORT
        || request.getStatus() != PrivacyRequestStatus.COMPLETED) {
      throw new BusinessException("PRIVACY_EXPORT_FORBIDDEN", "导出包尚不可下载");
    }
    return new PrivacyExportResult(
        requestId,
        "application/json",
        exportStore.load(requestId, userId),
        request.getUpdatedAt().plusDays(7).toInstant(ZoneOffset.UTC));
  }

  @Override
  @Transactional
  public void completeCorrection(long userId, long requestId, long ticketId) {
    PrivacyRequest request =
        repository
            .findPrivacyRequest(requestId)
            .orElseThrow(
                () -> new BusinessException("PRIVACY_REQUEST_NOT_FOUND", "隐私请求不存在"));
    if (request.getUserId() != userId || request.getType() != PrivacyRequestType.CORRECTION) {
      throw new BusinessException("PRIVACY_MANUAL_COMPLETION_INVALID", "隐私更正请求与工单用户不匹配");
    }
    long previousVersion = request.getVersion();
    request.completeManual("support-ticket:" + ticketId, utc(clock.instant()));
    if (!repository.updatePrivacyRequest(request, previousVersion)) {
      throw new BusinessException("PRIVACY_REQUEST_CONFLICT", "隐私请求正在被处理");
    }
  }

  @Override
  @Transactional
  public void resumeAccountClosure(long userId, long requestId, long ticketId) {
    PrivacyRequest request =
        repository
            .findPrivacyRequest(requestId)
            .orElseThrow(
                () -> new BusinessException("PRIVACY_REQUEST_NOT_FOUND", "隐私请求不存在"));
    if (request.getUserId() != userId || request.getType() != PrivacyRequestType.CLOSE_ACCOUNT) {
      throw new BusinessException("PRIVACY_MANUAL_RESUME_INVALID", "注销请求与工单用户不匹配");
    }
    long previousVersion = request.getVersion();
    request.resumeManual("support-ticket:" + ticketId, utc(clock.instant()));
    if (!repository.updatePrivacyRequest(request, previousVersion)) {
      throw new BusinessException("PRIVACY_REQUEST_CONFLICT", "隐私请求正在被处理");
    }
    jobScheduler.schedule(
        JOB_TYPE, requestId + ":manual:" + ticketId, payload(requestId), 20);
  }

  private void validate(CreatePrivacyRequestCommand command) {
    if (command == null
        || command.userId() <= 0
        || command.type() == null
        || blank(command.requestKey())
        || blank(command.scopeJson())) {
      throw new BusinessException("PRIVACY_INVALID_REQUEST", "隐私请求参数不完整");
    }
  }

  private String payload(long requestId) {
    return "{\"requestId\":" + requestId + "}";
  }

  private PrivacyRequestResult result(PrivacyRequest r) {
    return new PrivacyRequestResult(
        r.getId(),
        r.getType(),
        r.getStatus(),
        r.getProgress(),
        r.getDeadline().toInstant(ZoneOffset.UTC),
        r.getResultReference());
  }

  private String digest(String value) {
    try {
      return HexFormat.of()
          .formatHex(
              MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
  }

  private LocalDateTime utc(Instant value) {
    return LocalDateTime.ofInstant(value, ZoneOffset.UTC);
  }

  private boolean blank(String value) {
    return value == null || value.isBlank();
  }
}
