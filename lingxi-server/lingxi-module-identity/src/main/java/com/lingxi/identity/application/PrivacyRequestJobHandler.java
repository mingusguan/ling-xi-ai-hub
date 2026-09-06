package com.lingxi.identity.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lingxi.identity.api.*;
import com.lingxi.identity.domain.*;
import com.lingxi.kernel.*;
import java.time.*;
import java.util.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/** 按模块执行隐私请求；任务重试时各贡献者必须保持幂等。 */
@Component
@Slf4j
public class PrivacyRequestJobHandler implements AsyncJobHandler {
  private static final Set<String> REQUIRED_DATA_MODULES =
      Set.of(
          "identity",
          "goal",
          "companion",
          "engagement",
          "relationship",
          "content",
          "commerce",
          "operations");
  private final IdentitySecurityRepository repository;
  private final IdentityFacade identityFacade;
  private final List<PrivacyDataContributor> contributors;
  private final ObjectMapper objectMapper;
  private final PrivacyExportStore exportStore;
  private final PrivacyScopeParser scopeParser;
  private final PrivacyModuleTransactionExecutor moduleTransactionExecutor;
  private final PrivacyExportSanitizer exportSanitizer;
  private final PrivacyManualReviewPort manualReviewPort;
  private final Clock clock = Clock.systemUTC();

  public PrivacyRequestJobHandler(
      IdentitySecurityRepository repository,
      IdentityFacade identityFacade,
      List<PrivacyDataContributor> contributors,
      ObjectMapper objectMapper,
      PrivacyExportStore exportStore,
      PrivacyScopeParser scopeParser,
      PrivacyModuleTransactionExecutor moduleTransactionExecutor,
      PrivacyExportSanitizer exportSanitizer,
      List<PrivacyManualReviewPort> manualReviewPorts) {
    this.repository = repository;
    this.identityFacade = identityFacade;
    this.objectMapper = objectMapper;
    this.exportStore = exportStore;
    this.scopeParser = scopeParser;
    this.moduleTransactionExecutor = moduleTransactionExecutor;
    this.exportSanitizer = exportSanitizer;
    this.manualReviewPort = manualReviewPorts.stream().findFirst().orElse(null);
    this.contributors =
        contributors.stream()
            .sorted(Comparator.comparing(PrivacyDataContributor::moduleName))
            .toList();
  }

  @Override
  public boolean supports(String jobType) {
    return PrivacyApplicationService.JOB_TYPE.equals(jobType);
  }

  @Override
  public String handle(AsyncJobMessage message) throws Exception {
    JsonNode payload = objectMapper.readTree(message.payloadJson());
    long requestId = payload.path("requestId").asLong();
    PrivacyRequest request =
        repository
            .findPrivacyRequest(requestId)
            .orElseThrow(() -> new BusinessException("PRIVACY_REQUEST_NOT_FOUND", "隐私请求不存在"));
    if (request.getStatus() == PrivacyRequestStatus.COMPLETED
        || request.getStatus() == PrivacyRequestStatus.CANCELLED
        || request.getStatus() == PrivacyRequestStatus.WAITING_MANUAL) {
      return "{\"status\":\"ignored\"}";
    }
    PrivacyScope scope = scopeParser.parse(request.getScopeJson(), request.getType());
    List<PrivacyDataContributor> selected = selectedContributors(request.getType(), scope);
    requireCompleteCoverage(request.getType(), scope, selected);
    update(request, () -> request.start(now()));
    try {
      Map<String, Object> exported = new LinkedHashMap<>();
      PrivacyProcessingContext context =
          new PrivacyProcessingContext(
              request.getId(), request.getUserId(), request.getType(), scope);
      for (int index = 0; index < selected.size(); index++) {
        PrivacyDataContributor contributor = selected.get(index);
        PrivacyContribution contribution =
            isDeletion(request.getType())
                ? moduleTransactionExecutor.executeDeletion(contributor, context, now())
                : contributor.process(context);
        if (contribution.pendingReference() != null) {
          update(request, () -> request.awaitManual(contribution.pendingReference(), now()));
          return "{\"status\":\"waiting_manual\",\"requestId\":" + requestId + "}";
        }
        if (request.getType() == PrivacyRequestType.EXPORT && contribution.exportJson() != null) {
          exported.put(
              contributor.moduleName(),
              exportSanitizer.sanitize(
                  contributor.moduleName(), objectMapper.readTree(contribution.exportJson())));
        }
        int nextProgress = Math.min(99, 1 + ((index + 1) * 90 / selected.size()));
        update(request, () -> request.checkpoint(nextProgress, now()));
      }
      String reference =
          request.getType() == PrivacyRequestType.EXPORT
              ? exportStore.store(
                  request.getId(), request.getUserId(), objectMapper.writeValueAsString(exported))
              : null;
      if (request.getType() == PrivacyRequestType.CLOSE_ACCOUNT) {
        identityFacade.completeClosing(request.getUserId(), request.getId());
      }
      update(request, () -> request.complete(reference, now()));
      if (request.getType() == PrivacyRequestType.CLOSE_ACCOUNT) {
        repository.minimizeClosedPrivacyRequest(requestId);
      }
      return "{\"status\":\"completed\",\"requestId\":" + requestId + "}";
    } catch (Exception exception) {
      if (message.attempt() >= 20 && isCommerceBlocker(exception) && manualReviewPort != null) {
        String reference =
            manualReviewPort.open(
                requestId,
                request.getUserId(),
                "ACCOUNT_CLOSURE_TRANSACTION",
                "交易、退款、争议或续费取消在自动重试后仍未达到注销终态");
        update(request, () -> request.awaitManual(reference, now()));
        return "{\"status\":\"waiting_manual\",\"requestId\":" + requestId + "}";
      }
      failSafely(requestId, exception);
      throw exception;
    }
  }

  void requireCompleteCoverage(
      PrivacyRequestType requestType,
      PrivacyScope scope,
      List<PrivacyDataContributor> selectedContributors) {
    if (requestType == PrivacyRequestType.CORRECTION) {
      if (selectedContributors.stream().noneMatch(c -> "operations".equals(c.moduleName()))) {
        throw new BusinessException("PRIVACY_CORRECTION_HANDLER_MISSING", "隐私更正人工处理模块未配置");
      }
      return;
    }
    Set<String> available = new HashSet<>();
    for (PrivacyDataContributor contributor : selectedContributors) {
      if (!available.add(contributor.moduleName())) {
        throw new BusinessException("PRIVACY_CONTRIBUTOR_DUPLICATED", "隐私处理模块名称重复");
      }
    }
    Set<String> missing =
        scope.isAllModules()
            ? new TreeSet<>(REQUIRED_DATA_MODULES)
            : new TreeSet<>(scope.modules());
    missing.removeAll(available);
    if (!missing.isEmpty()) {
      throw new BusinessException(
          "PRIVACY_COVERAGE_INCOMPLETE", "隐私处理覆盖不完整，缺少模块：" + String.join(",", missing));
    }
  }

  private List<PrivacyDataContributor> selectedContributors(
      PrivacyRequestType requestType, PrivacyScope scope) {
    if (requestType == PrivacyRequestType.CORRECTION) {
      return contributors.stream().filter(c -> "operations".equals(c.moduleName())).toList();
    }
    return contributors.stream().filter(c -> scope.includes(c.moduleName())).toList();
  }

  private boolean isDeletion(PrivacyRequestType requestType) {
    return requestType == PrivacyRequestType.DELETE_DATA
        || requestType == PrivacyRequestType.CLOSE_ACCOUNT;
  }

  private boolean isCommerceBlocker(Exception exception) {
    if (!(exception instanceof BusinessException businessException)) {
      return false;
    }
    return Set.of(
            "PRIVACY_COMMERCE_PENDING",
            "PRIVACY_REFUND_PENDING",
            "PRIVACY_SUBSCRIPTION_CANCEL_PENDING")
        .contains(businessException.getCode());
  }

  private void update(PrivacyRequest request, Runnable mutation) {
    long previous = request.getVersion();
    mutation.run();
    if (!repository.updatePrivacyRequest(request, previous)) {
      throw new BusinessException("PRIVACY_REQUEST_CONFLICT", "隐私请求正在被处理");
    }
  }

  private void failSafely(long requestId, Exception cause) {
    try {
      PrivacyRequest latest = repository.findPrivacyRequest(requestId).orElse(null);
      if (latest == null
          || latest.getStatus() == PrivacyRequestStatus.COMPLETED
          || latest.getStatus() == PrivacyRequestStatus.CANCELLED
          || latest.getStatus() == PrivacyRequestStatus.WAITING_MANUAL) {
        return;
      }
      update(latest, () -> latest.fail(cause.getMessage(), now()));
    } catch (RuntimeException failureUpdateException) {
      log.error(
          "Failed to persist privacy request failure state, requestId={}",
          requestId,
          failureUpdateException);
    }
  }

  private LocalDateTime now() {
    return LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC);
  }
}
