package com.lingxi.content.infrastructure.privacy;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lingxi.content.application.ObjectStorageAdapter;
import com.lingxi.content.infrastructure.persistence.*;
import com.lingxi.identity.api.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** 处理用户文件、引用和导入导出任务的隐私请求，保留公共模板数据。 */
@Component
public class ContentPrivacyDataContributor implements PrivacyDataContributor {
  private final ContentFileMapper fileMapper;
  private final ContentFileReferenceMapper referenceMapper;
  private final ContentFileDerivativeMapper derivativeMapper;
  private final ContentImportJobMapper importMapper;
  private final ContentExportJobMapper exportMapper;
  private final ObjectMapper objectMapper;
  private final List<ObjectStorageAdapter> storages;

  public ContentPrivacyDataContributor(
      ContentFileMapper fileMapper,
      ContentFileReferenceMapper referenceMapper,
      ContentFileDerivativeMapper derivativeMapper,
      ContentImportJobMapper importMapper,
      ContentExportJobMapper exportMapper,
      ObjectMapper objectMapper,
      List<ObjectStorageAdapter> storages) {
    this.fileMapper = fileMapper;
    this.referenceMapper = referenceMapper;
    this.derivativeMapper = derivativeMapper;
    this.importMapper = importMapper;
    this.exportMapper = exportMapper;
    this.objectMapper = objectMapper;
    this.storages = List.copyOf(storages);
  }

  @Override public String moduleName() { return "content"; }

  @Override
  @Transactional
  public PrivacyContribution process(PrivacyProcessingContext context) {
    long userId = context.userId();
    PrivacyRequestType type = context.type();
    if (type == PrivacyRequestType.EXPORT) {
      return PrivacyContribution.exported(export(userId));
    }
    if (type != PrivacyRequestType.DELETE_DATA && type != PrivacyRequestType.CLOSE_ACCOUNT) {
      return PrivacyContribution.unchanged();
    }
    List<ContentFileEntity> files = fileMapper.selectList(
        Wrappers.<ContentFileEntity>lambdaQuery()
            .select(ContentFileEntity::getId, ContentFileEntity::getObjectKey)
            .eq(ContentFileEntity::getOwnerUserId, userId));
    List<Long> fileIds = files.stream().map(ContentFileEntity::getId).toList();
    List<String> objectKeys = new ArrayList<>();
    files.stream().map(ContentFileEntity::getObjectKey).forEach(objectKeys::add);
    if (!fileIds.isEmpty()) {
      derivativeMapper.selectList(
              Wrappers.<ContentFileDerivativeEntity>lambdaQuery()
                  .select(ContentFileDerivativeEntity::getObjectKey)
                  .in(ContentFileDerivativeEntity::getFileId, fileIds))
          .stream().map(ContentFileDerivativeEntity::getObjectKey).forEach(objectKeys::add);
    }
    for (ObjectStorageAdapter storage : storages) {
      String receipt =
          storage.logicallyIsolateUserObjects(context.requestId(), userId, List.copyOf(objectKeys));
      if (receipt == null || receipt.isBlank()) {
        throw new IllegalStateException("对象存储未返回逻辑隔离凭证");
      }
    }
    int affectedRows = 0;
    if (!fileIds.isEmpty()) {
      affectedRows += derivativeMapper.delete(
          Wrappers.<ContentFileDerivativeEntity>lambdaQuery()
              .in(ContentFileDerivativeEntity::getFileId, fileIds));
      affectedRows += referenceMapper.delete(
          Wrappers.<ContentFileReferenceEntity>lambdaQuery()
              .in(ContentFileReferenceEntity::getFileId, fileIds));
      affectedRows += fileMapper.deleteByIds(fileIds);
    }
    affectedRows += importMapper.delete(
        Wrappers.<ContentImportJobEntity>lambdaQuery().eq(ContentImportJobEntity::getUserId, userId));
    affectedRows += exportMapper.delete(
        Wrappers.<ContentExportJobEntity>lambdaQuery().eq(ContentExportJobEntity::getUserId, userId));
    return PrivacyContribution.deleted(affectedRows);
  }

  private String export(long userId) {
    Map<String, Object> data = new LinkedHashMap<>();
    List<ContentFileEntity> files = fileMapper.selectList(
        Wrappers.<ContentFileEntity>lambdaQuery()
            .select(
                ContentFileEntity::getId,
                ContentFileEntity::getPublicId,
                ContentFileEntity::getOwnerUserId,
                ContentFileEntity::getPurpose,
                ContentFileEntity::getOriginalName,
                ContentFileEntity::getSizeBytes,
                ContentFileEntity::getMimeType,
                ContentFileEntity::getSensitivity,
                ContentFileEntity::getStatus,
                ContentFileEntity::getCreatedAt,
                ContentFileEntity::getUpdatedAt)
            .eq(ContentFileEntity::getOwnerUserId, userId));
    List<Long> fileIds = files.stream().map(ContentFileEntity::getId).toList();
    data.put("files", files);
    data.put("references", referenceMapper.selectList(
        Wrappers.<ContentFileReferenceEntity>lambdaQuery()
            .eq(ContentFileReferenceEntity::getOwnerUserId, userId)));
    data.put("derivatives", fileIds.isEmpty() ? List.of() : derivativeMapper.selectList(
        Wrappers.<ContentFileDerivativeEntity>lambdaQuery()
            .in(ContentFileDerivativeEntity::getFileId, fileIds)));
    data.put("imports", importMapper.selectList(
        Wrappers.<ContentImportJobEntity>lambdaQuery().eq(ContentImportJobEntity::getUserId, userId)));
    data.put("exports", exportMapper.selectList(
        Wrappers.<ContentExportJobEntity>lambdaQuery().eq(ContentExportJobEntity::getUserId, userId)));
    try {
      return objectMapper.writeValueAsString(data);
    } catch (JsonProcessingException e) {
      throw new IllegalStateException("内容数据导出失败", e);
    }
  }
}
