package com.lingxi.content.domain;

import java.util.*;

public interface ContentRepository {
  Optional<FileAsset> findFile(long id);

  Optional<FileAsset> findFileByRequestKey(String key);

  void insertFile(FileAsset file);

  boolean updateFile(FileAsset file, long version);

  boolean fileReferenceExists(long fileId, String resourceType, String resourceId);

  void insertFileReference(
      long id,
      long fileId,
      long owner,
      String resourceType,
      String resourceId,
      java.time.LocalDateTime now);

  long countFileReferences(long fileId);

  boolean deleteFileReference(
      long fileId, long ownerUserId, String resourceType, String resourceId);

  List<String> findDerivativeObjectKeys(long fileId);

  void markDerivativesDeleted(long fileId);

  Optional<TransferJob> findJob(long id);

  Optional<TransferJob> findJobByRequestKey(String key);

  void insertJob(TransferJob job);

  boolean updateJob(TransferJob job, long version);

  Optional<Long> findTemplateId(String key);

  void insertTemplate(long id, String key, String name, java.time.LocalDateTime now);

  Optional<TemplateVersion> findTemplateVersion(long id);

  void insertTemplateVersion(TemplateVersion version);

  boolean updateTemplateVersion(TemplateVersion version, long previous);

  List<TemplateVersion> publishedTemplates(String ageScope);

  List<TemplateVersion> allTemplateVersions(String status);
}
