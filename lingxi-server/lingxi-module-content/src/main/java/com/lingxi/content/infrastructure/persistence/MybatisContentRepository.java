package com.lingxi.content.infrastructure.persistence;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lingxi.content.domain.*;
import java.util.*;
import org.springframework.stereotype.Repository;

@Repository
public class MybatisContentRepository implements ContentRepository {
  private final ContentFileMapper files;
  private final ContentFileReferenceMapper references;
  private final ContentFileDerivativeMapper derivatives;
  private final ContentImportJobMapper imports;
  private final ContentExportJobMapper exports;
  private final ContentTemplateMapper templates;
  private final ContentTemplateVersionMapper versions;

  public MybatisContentRepository(
      ContentFileMapper f,
      ContentFileReferenceMapper references,
      ContentFileDerivativeMapper derivatives,
      ContentImportJobMapper i,
      ContentExportJobMapper e,
      ContentTemplateMapper t,
      ContentTemplateVersionMapper v) {
    files = f;
    this.references = references;
    this.derivatives = derivatives;
    imports = i;
    exports = e;
    templates = t;
    versions = v;
  }

  public Optional<FileAsset> findFile(long id) {
    return Optional.ofNullable(files.selectById(id)).map(this::file);
  }

  public Optional<FileAsset> findFileByRequestKey(String key) {
    return Optional.ofNullable(
            files.selectOne(
                Wrappers.<ContentFileEntity>lambdaQuery()
                    .eq(ContentFileEntity::getRequestKey, key)
                    .last("LIMIT 1")))
        .map(this::file);
  }

  @Override
  public long countFilesByOwner(long ownerUserId) {
    Long total =
        files.selectCount(
            Wrappers.<ContentFileEntity>lambdaQuery().eq(ContentFileEntity::getOwnerUserId, ownerUserId));
    return total == null ? 0L : total;
  }

  @Override
  public List<FileAsset> findFilesByOwner(long ownerUserId, int page, int pageSize) {
    long offset = (long) (page - 1) * pageSize;
    return files
        .selectList(
            Wrappers.<ContentFileEntity>lambdaQuery()
                .eq(ContentFileEntity::getOwnerUserId, ownerUserId)
                .orderByDesc(ContentFileEntity::getCreatedAt)
                .orderByDesc(ContentFileEntity::getId)
                .last("LIMIT " + pageSize + " OFFSET " + offset))
        .stream()
        .map(this::file)
        .toList();
  }

  public void insertFile(FileAsset f) {
    ContentFileEntity e = new ContentFileEntity();
    e.setId(f.getId());
    e.setPublicId(f.getPublicId());
    e.setRequestKey(f.getRequestKey());
    e.setOwnerUserId(f.getOwnerUserId());
    e.setPurpose(f.getPurpose());
    e.setOriginalName(f.getOriginalName());
    e.setObjectKey(f.getObjectKey());
    e.setContentHash(f.getContentHash());
    e.setSizeBytes(f.getSizeBytes());
    e.setMimeType(f.getMimeType());
    e.setSensitivity(f.getSensitivity());
    e.setStatus(f.getStatus().name());
    e.setScanResult(f.getScanResult());
    e.setVersion(f.getVersion());
    e.setCreatedAt(f.getCreatedAt());
    e.setUpdatedAt(f.getUpdatedAt());
    files.insert(e);
  }

  public boolean updateFile(FileAsset f, long v) {
    return files.update(
            null,
            Wrappers.<ContentFileEntity>lambdaUpdate()
                .eq(ContentFileEntity::getId, f.getId())
                .eq(ContentFileEntity::getVersion, v)
                .set(ContentFileEntity::getStatus, f.getStatus().name())
                .set(ContentFileEntity::getScanResult, f.getScanResult())
                .set(ContentFileEntity::getVersion, f.getVersion())
                .set(ContentFileEntity::getUpdatedAt, f.getUpdatedAt()))
        == 1;
  }

  public boolean fileReferenceExists(long fileId, String type, String resourceId) {
    return references.selectCount(
            Wrappers.<ContentFileReferenceEntity>lambdaQuery()
                .eq(ContentFileReferenceEntity::getFileId, fileId)
                .eq(ContentFileReferenceEntity::getResourceType, type)
                .eq(ContentFileReferenceEntity::getResourceId, resourceId))
        > 0;
  }

  public void insertFileReference(
      long id,
      long fileId,
      long owner,
      String type,
      String resourceId,
      java.time.LocalDateTime now) {
    ContentFileReferenceEntity e = new ContentFileReferenceEntity();
    e.setId(id);
    e.setFileId(fileId);
    e.setOwnerUserId(owner);
    e.setResourceType(type);
    e.setResourceId(resourceId);
    e.setCreatedAt(now);
    references.insert(e);
  }

  public long countFileReferences(long fileId) {
    return references.selectCount(
        Wrappers.<ContentFileReferenceEntity>lambdaQuery()
            .eq(ContentFileReferenceEntity::getFileId, fileId));
  }

  public boolean deleteFileReference(long fileId, long owner, String type, String resourceId) {
    return references.delete(
            Wrappers.<ContentFileReferenceEntity>lambdaQuery()
                .eq(ContentFileReferenceEntity::getFileId, fileId)
                .eq(ContentFileReferenceEntity::getOwnerUserId, owner)
                .eq(ContentFileReferenceEntity::getResourceType, type)
                .eq(ContentFileReferenceEntity::getResourceId, resourceId))
        > 0;
  }

  public List<String> findDerivativeObjectKeys(long fileId) {
    return derivatives
        .selectList(
            Wrappers.<ContentFileDerivativeEntity>lambdaQuery()
                .eq(ContentFileDerivativeEntity::getFileId, fileId)
                .ne(ContentFileDerivativeEntity::getStatus, "DELETED"))
        .stream()
        .map(ContentFileDerivativeEntity::getObjectKey)
        .toList();
  }

  public void markDerivativesDeleted(long fileId) {
    derivatives.update(
        null,
        Wrappers.<ContentFileDerivativeEntity>lambdaUpdate()
            .eq(ContentFileDerivativeEntity::getFileId, fileId)
            .set(ContentFileDerivativeEntity::getStatus, "DELETED"));
  }

  public Optional<TransferJob> findJob(long id) {
    ContentImportJobEntity i = imports.selectById(id);
    if (i != null) return Optional.of(job(i));
    return Optional.ofNullable(exports.selectById(id)).map(this::job);
  }

  public Optional<TransferJob> findJobByRequestKey(String key) {
    ContentImportJobEntity i =
        imports.selectOne(
            Wrappers.<ContentImportJobEntity>lambdaQuery()
                .eq(ContentImportJobEntity::getRequestKey, key)
                .last("LIMIT 1"));
    if (i != null) return Optional.of(job(i));
    return Optional.ofNullable(
            exports.selectOne(
                Wrappers.<ContentExportJobEntity>lambdaQuery()
                    .eq(ContentExportJobEntity::getRequestKey, key)
                    .last("LIMIT 1")))
        .map(this::job);
  }

  public void insertJob(TransferJob j) {
    if (j.getType() == TransferJob.Type.IMPORT) {
      ContentImportJobEntity e = new ContentImportJobEntity();
      e.setId(j.getId());
      e.setRequestKey(j.getRequestKey());
      e.setUserId(j.getUserId());
      e.setSourceFileId(j.getSourceFileId());
      e.setFormat(j.getFormat());
      e.setStatus(j.getStatus().name());
      e.setPreviewJson(j.getPreviewJson());
      e.setErrorJson(j.getErrorJson());
      e.setVersion(j.getVersion());
      e.setCreatedAt(j.getCreatedAt());
      e.setUpdatedAt(j.getUpdatedAt());
      imports.insert(e);
    } else {
      ContentExportJobEntity e = new ContentExportJobEntity();
      e.setId(j.getId());
      e.setRequestKey(j.getRequestKey());
      e.setUserId(j.getUserId());
      e.setScopeJson(j.getScopeJson());
      e.setFormat(j.getFormat());
      e.setStatus(j.getStatus().name());
      e.setResultFileId(j.getResultFileId());
      e.setExpiresAt(j.getExpiresAt());
      e.setErrorMessage(j.getErrorJson());
      e.setVersion(j.getVersion());
      e.setCreatedAt(j.getCreatedAt());
      e.setUpdatedAt(j.getUpdatedAt());
      exports.insert(e);
    }
  }

  public boolean updateJob(TransferJob j, long v) {
    if (j.getType() == TransferJob.Type.IMPORT)
      return imports.update(
              null,
              Wrappers.<ContentImportJobEntity>lambdaUpdate()
                  .eq(ContentImportJobEntity::getId, j.getId())
                  .eq(ContentImportJobEntity::getVersion, v)
                  .set(ContentImportJobEntity::getStatus, j.getStatus().name())
                  .set(ContentImportJobEntity::getPreviewJson, j.getPreviewJson())
                  .set(ContentImportJobEntity::getErrorJson, j.getErrorJson())
                  .set(ContentImportJobEntity::getVersion, j.getVersion())
                  .set(ContentImportJobEntity::getUpdatedAt, j.getUpdatedAt()))
          == 1;
    return exports.update(
            null,
            Wrappers.<ContentExportJobEntity>lambdaUpdate()
                .eq(ContentExportJobEntity::getId, j.getId())
                .eq(ContentExportJobEntity::getVersion, v)
                .set(ContentExportJobEntity::getStatus, j.getStatus().name())
                .set(ContentExportJobEntity::getResultFileId, j.getResultFileId())
                .set(ContentExportJobEntity::getExpiresAt, j.getExpiresAt())
                .set(ContentExportJobEntity::getErrorMessage, j.getErrorJson())
                .set(ContentExportJobEntity::getVersion, j.getVersion())
                .set(ContentExportJobEntity::getUpdatedAt, j.getUpdatedAt()))
        == 1;
  }

  public Optional<Long> findTemplateId(String key) {
    return Optional.ofNullable(
            templates.selectOne(
                Wrappers.<ContentTemplateEntity>lambdaQuery()
                    .eq(ContentTemplateEntity::getTemplateKey, key)
                    .last("LIMIT 1")))
        .map(ContentTemplateEntity::getId);
  }

  public void insertTemplate(long id, String key, String name, java.time.LocalDateTime now) {
    ContentTemplateEntity e = new ContentTemplateEntity();
    e.setId(id);
    e.setTemplateKey(key);
    e.setName(name);
    e.setStatus("ACTIVE");
    e.setCreatedAt(now);
    e.setUpdatedAt(now);
    templates.insert(e);
  }

  public Optional<TemplateVersion> findTemplateVersion(long id) {
    return Optional.ofNullable(versions.selectById(id)).map(this::version);
  }

  public void insertTemplateVersion(TemplateVersion v) {
    ContentTemplateVersionEntity e = new ContentTemplateVersionEntity();
    fill(e, v);
    versions.insert(e);
  }

  public boolean updateTemplateVersion(TemplateVersion v, long previous) {
    boolean ok =
        versions.update(
                null,
                Wrappers.<ContentTemplateVersionEntity>lambdaUpdate()
                    .eq(ContentTemplateVersionEntity::getId, v.getId())
                    .eq(ContentTemplateVersionEntity::getLockVersion, previous)
                    .set(ContentTemplateVersionEntity::getStatus, v.getStatus().name())
                    .set(ContentTemplateVersionEntity::getReviewerUserId, v.getReviewerUserId())
                    .set(ContentTemplateVersionEntity::getReviewReason, v.getReviewReason())
                    .set(ContentTemplateVersionEntity::getPublishedAt, v.getPublishedAt())
                    .set(ContentTemplateVersionEntity::getLockVersion, v.getVersion())
                    .set(ContentTemplateVersionEntity::getUpdatedAt, v.getUpdatedAt()))
            == 1;
    if (ok && v.getStatus() == TemplateVersion.Status.PUBLISHED)
      templates.update(
          null,
          Wrappers.<ContentTemplateEntity>lambdaUpdate()
              .eq(ContentTemplateEntity::getId, v.getTemplateId())
              .set(ContentTemplateEntity::getCurrentVersion, v.getVersionNo())
              .set(ContentTemplateEntity::getUpdatedAt, v.getUpdatedAt()));
    return ok;
  }

  public List<TemplateVersion> publishedTemplates(String age) {
    return versions
        .selectList(
            Wrappers.<ContentTemplateVersionEntity>lambdaQuery()
                .eq(ContentTemplateVersionEntity::getStatus, "PUBLISHED")
                .and(
                    q ->
                        q.eq(ContentTemplateVersionEntity::getAgeScope, age)
                            .or()
                            .eq(ContentTemplateVersionEntity::getAgeScope, "ALL"))
                .orderByDesc(ContentTemplateVersionEntity::getPublishedAt))
        .stream()
        .map(this::version)
        .toList();
  }

  public List<TemplateVersion> allTemplateVersions(String status) {
    var query = Wrappers.<ContentTemplateVersionEntity>lambdaQuery()
        .orderByDesc(ContentTemplateVersionEntity::getCreatedAt)
        .last("LIMIT 200");
    if (status != null && !status.isBlank()) query.eq(ContentTemplateVersionEntity::getStatus, status);
    return versions.selectList(query).stream().map(this::version).toList();
  }

  private FileAsset file(ContentFileEntity e) {
    return FileAsset.rehydrate(
        e.getId(),
        e.getPublicId(),
        e.getRequestKey(),
        e.getOwnerUserId(),
        e.getPurpose(),
        e.getOriginalName(),
        e.getObjectKey(),
        e.getContentHash(),
        e.getSizeBytes(),
        e.getMimeType(),
        e.getSensitivity(),
        FileAsset.Status.valueOf(e.getStatus()),
        e.getScanResult(),
        e.getVersion(),
        e.getCreatedAt(),
        e.getUpdatedAt());
  }

  private TransferJob job(ContentImportJobEntity e) {
    return TransferJob.rehydrate(
        e.getId(),
        e.getRequestKey(),
        e.getUserId(),
        TransferJob.Type.IMPORT,
        e.getSourceFileId(),
        null,
        e.getFormat(),
        TransferJob.Status.valueOf(e.getStatus()),
        e.getPreviewJson(),
        e.getErrorJson(),
        null,
        null,
        e.getVersion(),
        e.getCreatedAt(),
        e.getUpdatedAt());
  }

  private TransferJob job(ContentExportJobEntity e) {
    return TransferJob.rehydrate(
        e.getId(),
        e.getRequestKey(),
        e.getUserId(),
        TransferJob.Type.EXPORT,
        null,
        e.getScopeJson(),
        e.getFormat(),
        TransferJob.Status.valueOf(e.getStatus()),
        null,
        e.getErrorMessage(),
        e.getResultFileId(),
        e.getExpiresAt(),
        e.getVersion(),
        e.getCreatedAt(),
        e.getUpdatedAt());
  }

  private TemplateVersion version(ContentTemplateVersionEntity e) {
    return TemplateVersion.rehydrate(
        e.getId(),
        e.getTemplateId(),
        e.getVersionNo(),
        e.getAgeScope(),
        e.getContentSnapshot(),
        TemplateVersion.Status.valueOf(e.getStatus()),
        e.getReviewerUserId(),
        e.getReviewReason(),
        e.getPublishedAt(),
        e.getLockVersion(),
        e.getCreatedAt(),
        e.getUpdatedAt());
  }

  private void fill(ContentTemplateVersionEntity e, TemplateVersion v) {
    e.setId(v.getId());
    e.setTemplateId(v.getTemplateId());
    e.setVersionNo(v.getVersionNo());
    e.setAgeScope(v.getAgeScope());
    e.setContentSnapshot(v.getContentSnapshot());
    e.setStatus(v.getStatus().name());
    e.setReviewerUserId(v.getReviewerUserId());
    e.setReviewReason(v.getReviewReason());
    e.setPublishedAt(v.getPublishedAt());
    e.setLockVersion(v.getVersion());
    e.setCreatedAt(v.getCreatedAt());
    e.setUpdatedAt(v.getUpdatedAt());
  }
}
