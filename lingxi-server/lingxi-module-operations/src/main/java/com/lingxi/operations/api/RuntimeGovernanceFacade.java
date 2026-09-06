package com.lingxi.operations.api;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/** PC Web 与 HarmonyOS 共用的运行时治理视图。 */
public interface RuntimeGovernanceFacade {

  ClientBootstrapResult clientBootstrap(String platform, long currentVersionCode);

  record ClientBootstrapResult(
      UpgradePolicy upgrade,
      List<ComplianceDocument> documents,
      Map<String, Boolean> featureFlags,
      List<String> experiments,
      LocalDateTime generatedAt) {}

  record UpgradePolicy(
      boolean upgradeAvailable,
      boolean forceUpgrade,
      String versionName,
      long versionCode,
      long minimumVersionCode,
      String releaseNotes) {}

  record ComplianceDocument(
      String documentType,
      String versionNo,
      String title,
      String contentRef,
      String contentDigest,
      LocalDateTime effectiveAt) {}
}
