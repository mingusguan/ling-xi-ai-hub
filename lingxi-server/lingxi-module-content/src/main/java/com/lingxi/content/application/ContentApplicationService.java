package com.lingxi.content.application;

import com.lingxi.content.api.*;
import com.lingxi.content.domain.*;
import com.lingxi.identity.api.*;
import com.lingxi.kernel.*;
import java.time.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

/** 文件、模板和传输任务应用服务。 */
@Service
public class ContentApplicationService implements ContentFacade {
  public static final String SCAN = "content.file.scan",
      CLEANUP = "content.file.cleanup",
      IMPORT_PREVIEW = "content.import.preview",
      IMPORT_APPLY = "content.import.apply",
      EXPORT = "content.export";
  private final ContentRepository repo;
  private final IdentityFacade identities;
  private final IdGenerator ids;
  private final AsyncJobScheduler jobs;
  private final ObjectStorageAdapter storage;
  private final ContentAdminAuthorizationAdapter adminAuthorization;
  private final ContentTransactionService transactions;
  private final DomainEventPublisher events;
  private final TemplateGovernanceRepository templateGovernance;

  public ContentApplicationService(
      ContentRepository repo,
      IdentityFacade identities,
      IdGenerator ids,
      AsyncJobScheduler jobs,
      List<ObjectStorageAdapter> storage,
      List<ContentAdminAuthorizationAdapter> adminAuthorizations,
      ContentTransactionService transactions,
      DomainEventPublisher events,
      TemplateGovernanceRepository templateGovernance) {
    this.repo = repo;
    this.identities = identities;
    this.ids = ids;
    this.jobs = jobs;
    this.storage = storage.stream().findFirst().orElse(null);
    this.adminAuthorization = adminAuthorizations.stream().findFirst().orElse(null);
    this.transactions = transactions;
    this.events = events;
    this.templateGovernance = templateGovernance;
  }

  public UploadTicketResult createUploadTicket(CreateUploadTicketCommand c) {
    requireUser(c.ownerUserId());
    var existing = repo.findFileByRequestKey(c.requestKey());
    if (existing.isPresent()) {
      FileAsset f = existing.get();
      if (f.getOwnerUserId() != c.ownerUserId()
          || !f.getContentHash().equals(c.contentHash())
          || f.getSizeBytes() != c.sizeBytes()
          || !f.getPurpose().equals(c.purpose())
          || !f.getMimeType().equals(c.mimeType())
          || !f.getOriginalName().equals(c.originalName())
          || !f.getSensitivity().equals(c.sensitivity()))
        throw error("CONTENT_IDEMPOTENCY_CONFLICT", "幂等键对应不同文件");
      if (f.getStatus() != FileAsset.Status.UPLOADING) return ticket(f, null);
      if (storage == null) throw error("CONTENT_STORAGE_UNAVAILABLE", "私有对象存储尚未配置");
      Instant expires = Instant.now().plus(Duration.ofMinutes(15));
      String reference =
          storage
              .authorizePrivateUpload(f.getObjectKey(), f.getMimeType(), f.getSizeBytes(), expires)
              .opaqueReference();
      return new UploadTicketResult(
          f.getId(),
          f.getPublicId(),
          f.getObjectKey(),
          reference,
          expires,
          f.getStatus().name(),
          f.getVersion());
    }
    if (storage == null) throw error("CONTENT_STORAGE_UNAVAILABLE", "私有对象存储尚未配置");
    LocalDateTime now = now();
    long id = ids.nextId();
    String publicId = UUID.randomUUID().toString().replace("-", "");
    String objectKey = "private/" + c.ownerUserId() + "/" + publicId;
    FileAsset f =
        FileAsset.create(
            id,
            publicId,
            c.requestKey(),
            c.ownerUserId(),
            c.purpose(),
            c.originalName(),
            objectKey,
            c.contentHash(),
            c.sizeBytes(),
            c.mimeType(),
            c.sensitivity(),
            now);
    Instant expires = Instant.now().plus(Duration.ofMinutes(15));
    String reference =
        storage
            .authorizePrivateUpload(objectKey, c.mimeType(), c.sizeBytes(), expires)
            .opaqueReference();
    repo.insertFile(f);
    return new UploadTicketResult(
        id, publicId, objectKey, reference, expires, f.getStatus().name(), f.getVersion());
  }

  public FileResult markUploaded(long user, long id, long expected) {
    FileAsset f = file(id);
    if (storage == null) throw error("CONTENT_STORAGE_UNAVAILABLE", "私有对象存储尚未配置");
    var object = storage.verifyPrivateUpload(f.getObjectKey());
    if (object.sizeBytes() != f.getSizeBytes()
        || !object.mimeType().equals(f.getMimeType())
        || !object.contentHash().equalsIgnoreCase(f.getContentHash()))
      throw error("CONTENT_UPLOAD_MISMATCH", "上传对象与申请信息不一致");
    return result(transactions.beginScan(user, id, expected));
  }

  @Transactional
  public FileResult completeScan(long id, boolean safe, String detail, long expected) {
    FileAsset f = file(id);
    long v = f.getVersion();
    f.scanned(safe, detail, expected, now());
    updated(repo.updateFile(f, v));
    return result(f);
  }

  @Transactional
  public FileResult referenceReadyFile(ReferenceFileCommand c) {
    FileAsset f = file(c.fileId());
    f.assertReady(c.userId());
    if (c.resourceType() == null || c.resourceId() == null)
      throw error("CONTENT_INVALID_REFERENCE", "文件引用参数不完整");
    if (!repo.fileReferenceExists(c.fileId(), c.resourceType(), c.resourceId()))
      repo.insertFileReference(
          ids.nextId(), c.fileId(), c.userId(), c.resourceType(), c.resourceId(), now());
    return result(f);
  }

  @Transactional(readOnly = true)
  public FileResult getReadyFile(long user, long id) {
    FileAsset f = file(id);
    f.assertReady(user);
    return result(f);
  }

  @Transactional
  public FileResult removeReference(long user, long id, String type, String resourceId) {
    FileAsset file = file(id);
    file.assertReady(user);
    if (type == null || type.isBlank() || resourceId == null || resourceId.isBlank()) {
      throw error("CONTENT_INVALID_REFERENCE", "文件引用参数不完整");
    }
    repo.deleteFileReference(id, user, type, resourceId);
    return result(file);
  }

  public FileResult deleteFile(long user, long id, long expectedVersion) {
    requireUser(user);
    return result(transactions.beginDeletion(user, id, expectedVersion));
  }

  public FileResult completeDeletion(long id, long expectedVersion) {
    return result(transactions.completeDeletion(id, expectedVersion));
  }

  @Transactional(readOnly = true)
  public List<String> derivativeObjectKeys(long fileId) {
    return repo.findDerivativeObjectKeys(fileId);
  }

  @Transactional
  public TransferJobResult createImport(CreateImportJobCommand c) {
    requireUser(c.userId());
    file(c.sourceFileId()).assertReady(c.userId());
    var existing = repo.findJobByRequestKey(c.requestKey());
    if (existing.isPresent()) {
      TransferJob j = existing.get();
      if (j.getType() != TransferJob.Type.IMPORT
          || j.getUserId() != c.userId()
          || !Objects.equals(j.getSourceFileId(), c.sourceFileId())
          || !Objects.equals(j.getFormat(), c.format()))
        throw error("CONTENT_IDEMPOTENCY_CONFLICT", "幂等键对应不同导入任务");
      return transfer(j);
    }
    TransferJob j =
        TransferJob.importJob(
            ids.nextId(), c.requestKey(), c.userId(), c.sourceFileId(), c.format(), now());
    repo.insertJob(j);
    jobs.schedule(IMPORT_PREVIEW, String.valueOf(j.getId()), "{\"jobId\":" + j.getId() + "}", 3);
    return transfer(j);
  }

  @Transactional
  public TransferJobResult saveImportPreview(
      long user, long id, String preview, String error, long expected) {
    TransferJob j = job(id);
    long v = j.getVersion();
    j.preview(user, preview, error, expected, now());
    updated(repo.updateJob(j, v));
    return transfer(j);
  }

  @Transactional
  public TransferJobResult confirmImport(long user, long id, long expected) {
    TransferJob j = job(id);
    long v = j.getVersion();
    j.confirm(user, expected, now());
    updated(repo.updateJob(j, v));
    jobs.schedule(IMPORT_APPLY, String.valueOf(id), "{\"jobId\":" + id + "}", 3);
    return transfer(j);
  }

  @Transactional
  public TransferJobResult createExport(CreateExportJobCommand c) {
    requireUser(c.userId());
    if (!c.recentAuthentication()) throw error("CONTENT_RECENT_AUTH_REQUIRED", "导出需要近期认证");
    var existing = repo.findJobByRequestKey(c.requestKey());
    if (existing.isPresent()) {
      TransferJob j = existing.get();
      if (j.getType() != TransferJob.Type.EXPORT
          || j.getUserId() != c.userId()
          || !Objects.equals(j.getScopeJson(), c.scopeJson())
          || !Objects.equals(j.getFormat(), c.format()))
        throw error("CONTENT_IDEMPOTENCY_CONFLICT", "幂等键对应不同导出任务");
      return transfer(j);
    }
    TransferJob j =
        TransferJob.exportJob(
            ids.nextId(), c.requestKey(), c.userId(), c.scopeJson(), c.format(), now());
    repo.insertJob(j);
    jobs.schedule(EXPORT, String.valueOf(j.getId()), "{\"jobId\":" + j.getId() + "}", 3);
    return transfer(j);
  }

  @Transactional
  public TransferJobResult completeExport(long id, Long fileId, String error, long expected) {
    TransferJob j = job(id);
    if (error == null) {
      if (fileId == null) throw error("CONTENT_EXPORT_FILE_REQUIRED", "导出结果文件缺失");
      file(fileId).assertReady(j.getUserId());
    }
    long v = j.getVersion();
    j.complete(fileId, error, expected, now());
    updated(repo.updateJob(j, v));
    return transfer(j);
  }

  @Transactional
  public TransferJobResult completeImport(long id, String error, long expected) {
    TransferJob j = job(id);
    long v = j.getVersion();
    j.completeImport(error, expected, now());
    updated(repo.updateJob(j, v));
    return transfer(j);
  }

  @Transactional
  public TransferJobResult getTransfer(long user, long id) {
    TransferJob j = job(id);
    j.owner(user);
    if (j.getStatus() == TransferJob.Status.COMPLETED
        && j.getExpiresAt() != null
        && !j.getExpiresAt().isAfter(Instant.now())) {
      long old = j.getVersion();
      j.expire(user, old, now());
      updated(repo.updateJob(j, old));
    }
    return transfer(j);
  }

  @Transactional
  public TemplateVersionResult createTemplate(CreateTemplateVersionCommand c) {
    requireAdmin(c.operatorUserId(), "content:template:create");
    LocalDateTime now = now();
    long templateId =
        repo.findTemplateId(c.templateKey())
            .orElseGet(
                () -> {
                  long id = ids.nextId();
                  repo.insertTemplate(id, c.templateKey(), c.name(), now);
                  return id;
                });
    TemplateVersion v =
        TemplateVersion.create(
            ids.nextId(), templateId, c.versionNo(), c.ageScope(), c.contentSnapshot(), now);
    repo.insertTemplateVersion(v);
    return template(v);
  }

  @Transactional
  public TemplateVersionResult submitTemplate(long operator, long id, long expected) {
    requireAdmin(operator, "content:template:submit");
    TemplateVersion v = version(id);
    long old = v.getVersion();
    v.submit(expected, now());
    updated(repo.updateTemplateVersion(v, old));
    return template(v);
  }

  @Transactional
  public TemplateVersionResult reviewTemplate(TemplateReviewCommand command) {
    requireHighRisk(command.reviewerId(), "content:template:review",
        command.recentAuthentication(), command.operationReason(), command.ticketNo());
    TemplateVersion v = version(command.versionId());
    if (command.approved() && !templateGovernance.allApproved(v.getId(), v.getVersion())) {
      throw error("CONTENT_TEMPLATE_REVIEW_INCOMPLETE", "安全性、可行性和质量评审全部通过后才能发布");
    }
    long old = v.getVersion();
    v.review(command.reviewerId(), command.approved(), command.reviewReason(),
        command.expectedVersion(), now());
    updated(repo.updateTemplateVersion(v, old));
    audit(command.reviewerId(), "TEMPLATE_REVIEW", v, command.operationReason(), command.ticketNo());
    return template(v);
  }

  @Transactional
  public TemplateVersionResult retireTemplate(TemplateRetireCommand command) {
    requireHighRisk(command.operatorId(), "content:template:retire",
        command.recentAuthentication(), command.operationReason(), command.ticketNo());
    TemplateVersion v = version(command.versionId());
    long old = v.getVersion();
    v.retire(command.expectedVersion(), now());
    updated(repo.updateTemplateVersion(v, old));
    audit(command.operatorId(), "TEMPLATE_RETIRE", v, command.operationReason(), command.ticketNo());
    return template(v);
  }

  @Transactional(readOnly = true)
  public List<TemplateVersionResult> listAdminTemplates(long operator, String status) {
    requireAdmin(operator, "content:template:read");
    if (status != null && !status.isBlank()) {
      try {
        TemplateVersion.Status.valueOf(status);
      } catch (IllegalArgumentException exception) {
        throw error("CONTENT_TEMPLATE_STATUS_INVALID", "模板状态不合法");
      }
    }
    return repo.allTemplateVersions(status).stream().map(this::template).toList();
  }

  @Transactional(readOnly = true)
  public List<TemplateVersionResult> listPublishedTemplates(long userId) {
    AccessProfile profile = identities.getAccessProfile(userId);
    if (!profile.coreFeaturesAllowed() || profile.ageBand() == AgeBand.UNDER_14) {
      throw error("CONTENT_ACCOUNT_RESTRICTED", "当前账号不可使用内容功能");
    }
    return repo.publishedTemplates(profile.ageBand().name()).stream().map(this::template).toList();
  }

  @Transactional(readOnly = true)
  public FileAsset loadFile(long id) {
    return file(id);
  }

  @Transactional(readOnly = true)
  public TransferJob loadJob(long id) {
    return job(id);
  }

  private void requireUser(long id) {
    AccessProfile p = identities.getAccessProfile(id);
    if (!p.coreFeaturesAllowed() || p.ageBand() == AgeBand.UNDER_14)
      throw error("CONTENT_ACCOUNT_RESTRICTED", "当前账号不可使用内容功能");
  }

  private void requireAdmin(long adminId, String permission) {
    if (adminAuthorization == null || !adminAuthorization.allowed(adminId, permission))
      throw error("CONTENT_ADMIN_FORBIDDEN", "管理员权限不足");
  }

  private void requireHighRisk(long adminId, String permission, boolean recent,
      String reason, String ticketNo) {
    requireAdmin(adminId, permission);
    if (!recent || reason == null || reason.isBlank() || ticketNo == null || ticketNo.isBlank()) {
      throw error("CONTENT_HIGH_RISK_CONTEXT_REQUIRED", "模板高风险操作需要近期认证、原因和工单号");
    }
  }

  private void audit(long adminId, String action, TemplateVersion version,
      String reason, String ticketNo) {
    events.publish(new AdminActionAuditedEvent(UUID.randomUUID().toString(), adminId, action,
        "TEMPLATE_VERSION", String.valueOf(version.getId()), version.getVersion(), reason,
        ticketNo, Instant.now()));
  }

  private FileAsset file(long id) {
    return repo.findFile(id).orElseThrow(() -> error("CONTENT_FILE_NOT_FOUND", "文件不存在"));
  }

  private TransferJob job(long id) {
    return repo.findJob(id).orElseThrow(() -> error("CONTENT_JOB_NOT_FOUND", "任务不存在"));
  }

  private TemplateVersion version(long id) {
    return repo.findTemplateVersion(id)
        .orElseThrow(() -> error("CONTENT_TEMPLATE_NOT_FOUND", "模板版本不存在"));
  }

  private UploadTicketResult ticket(FileAsset f, String ref) {
    return new UploadTicketResult(
        f.getId(),
        f.getPublicId(),
        f.getObjectKey(),
        ref,
        null,
        f.getStatus().name(),
        f.getVersion());
  }

  private FileResult result(FileAsset f) {
    return new FileResult(
        f.getId(),
        f.getPublicId(),
        f.getOwnerUserId(),
        f.getPurpose(),
        f.getOriginalName(),
        f.getSizeBytes(),
        f.getMimeType(),
        f.getContentHash(),
        f.getSensitivity(),
        f.getStatus().name(),
        f.getScanResult(),
        f.getVersion());
  }

  private TransferJobResult transfer(TransferJob j) {
    return new TransferJobResult(
        j.getId(),
        j.getType().name(),
        j.getUserId(),
        j.getStatus().name(),
        j.getPreviewJson(),
        j.getErrorJson(),
        j.getResultFileId(),
        j.getExpiresAt(),
        j.getVersion());
  }

  private TemplateVersionResult template(TemplateVersion v) {
    return new TemplateVersionResult(
        v.getId(),
        v.getTemplateId(),
        v.getVersionNo(),
        v.getAgeScope(),
        v.getContentSnapshot(),
        v.getStatus().name(),
        v.getReviewerUserId(),
        v.getReviewReason(),
        v.getPublishedAt(),
        v.getVersion());
  }

  private LocalDateTime now() {
    return LocalDateTime.now(ZoneOffset.UTC);
  }

  private void updated(boolean ok) {
    if (!ok) throw error("CONTENT_CONCURRENT_UPDATE", "数据已被并发修改");
  }

  private BusinessException error(String c, String m) {
    return new BusinessException(c, m);
  }
}
