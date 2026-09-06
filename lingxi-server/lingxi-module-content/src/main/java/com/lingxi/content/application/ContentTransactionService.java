package com.lingxi.content.application;

import com.lingxi.content.domain.*;
import com.lingxi.kernel.*;
import java.time.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

/** 负责文件状态和异步任务登记的一致本地事务。 */
@Service
public class ContentTransactionService {
  private final ContentRepository repository;
  private final AsyncJobScheduler jobs;

  public ContentTransactionService(ContentRepository repository, AsyncJobScheduler jobs) {
    this.repository = repository;
    this.jobs = jobs;
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public FileAsset beginScan(long userId, long fileId, long expectedVersion) {
    FileAsset file =
        repository
            .findFile(fileId)
            .orElseThrow(() -> new BusinessException("CONTENT_FILE_NOT_FOUND", "文件不存在"));
    long previousVersion = file.getVersion();
    LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
    file.uploaded(userId, expectedVersion, now);
    file.scanning(file.getVersion(), now);
    if (!repository.updateFile(file, previousVersion)) {
      throw new BusinessException("CONTENT_CONCURRENT_UPDATE", "文件已被并发修改");
    }
    jobs.schedule(
        ContentApplicationService.SCAN, String.valueOf(fileId), "{\"fileId\":" + fileId + "}", 5);
    return file;
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public FileAsset beginDeletion(long userId, long fileId, long expectedVersion) {
    FileAsset file = load(fileId);
    file.assertOwned(userId);
    if (repository.countFileReferences(fileId) > 0) {
      throw new BusinessException("CONTENT_FILE_IN_USE", "文件仍被业务资源引用");
    }
    if (file.getStatus() == FileAsset.Status.DELETING
        || file.getStatus() == FileAsset.Status.DELETED) return file;
    long previousVersion = file.getVersion();
    file.beginDeletion(userId, expectedVersion, now());
    update(file, previousVersion);
    jobs.schedule(
        ContentApplicationService.CLEANUP,
        String.valueOf(fileId),
        "{\"fileId\":" + fileId + "}",
        8);
    return file;
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public FileAsset completeDeletion(long fileId, long expectedVersion) {
    FileAsset file = load(fileId);
    if (file.getStatus() == FileAsset.Status.DELETED) return file;
    long previousVersion = file.getVersion();
    file.completeDeletion(expectedVersion, now());
    repository.markDerivativesDeleted(fileId);
    update(file, previousVersion);
    return file;
  }

  private FileAsset load(long fileId) {
    return repository
        .findFile(fileId)
        .orElseThrow(() -> new BusinessException("CONTENT_FILE_NOT_FOUND", "文件不存在"));
  }

  private void update(FileAsset file, long previousVersion) {
    if (!repository.updateFile(file, previousVersion)) {
      throw new BusinessException("CONTENT_CONCURRENT_UPDATE", "文件已被并发修改");
    }
  }

  private LocalDateTime now() {
    return LocalDateTime.now(ZoneOffset.UTC);
  }
}
