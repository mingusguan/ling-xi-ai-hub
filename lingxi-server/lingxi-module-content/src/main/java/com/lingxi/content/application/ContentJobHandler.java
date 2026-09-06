package com.lingxi.content.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lingxi.content.domain.*;
import com.lingxi.kernel.*;
import java.util.*;
import org.springframework.stereotype.Component;

/** 可恢复的文件扫描和传输任务处理器。 */
@Component
public class ContentJobHandler implements AsyncJobHandler {
  private final ContentApplicationService service;
  private final ContentScanAdapter scanner;
  private final ContentTransferAdapter transfer;
  private final ObjectMapper json;
  private final ObjectStorageAdapter storage;

  public ContentJobHandler(
      ContentApplicationService service,
      List<ContentScanAdapter> scanners,
      List<ContentTransferAdapter> transfers,
      List<ObjectStorageAdapter> storages,
      ObjectMapper json) {
    this.service = service;
    scanner = scanners.stream().findFirst().orElse(null);
    transfer = transfers.stream().findFirst().orElse(null);
    storage = storages.stream().findFirst().orElse(null);
    this.json = json;
  }

  public boolean supports(String t) {
    return Set.of(
            ContentApplicationService.SCAN,
            ContentApplicationService.CLEANUP,
            ContentApplicationService.IMPORT_PREVIEW,
            ContentApplicationService.IMPORT_APPLY,
            ContentApplicationService.EXPORT)
        .contains(t);
  }

  public String handle(AsyncJobMessage m) throws Exception {
    long id =
        json.readTree(m.payloadJson())
            .path(m.jobType().equals(ContentApplicationService.SCAN) ? "fileId" : "jobId")
            .asLong();
    if (m.jobType().equals(ContentApplicationService.CLEANUP)) {
      if (storage == null) throw new BusinessException("CONTENT_STORAGE_UNAVAILABLE", "私有对象存储尚未配置");
      FileAsset file = service.loadFile(id);
      if (file.getStatus() != FileAsset.Status.DELETED) {
        storage.deletePrivateObject(file.getObjectKey());
        for (String objectKey : service.derivativeObjectKeys(id)) {
          storage.deletePrivateObject(objectKey);
        }
        service.completeDeletion(id, file.getVersion());
      }
    } else if (m.jobType().equals(ContentApplicationService.SCAN)) {
      if (scanner == null) throw new BusinessException("CONTENT_SCANNER_UNAVAILABLE", "内容扫描器尚未配置");
      FileAsset f = service.loadFile(id);
      var v = scanner.scan(f);
      service.completeScan(id, v.safe(), v.detail(), f.getVersion());
    } else {
      if (transfer == null)
        throw new BusinessException("CONTENT_TRANSFER_UNAVAILABLE", "内容传输处理器尚未配置");
      TransferJob j = service.loadJob(id);
      if (m.jobType().equals(ContentApplicationService.IMPORT_PREVIEW)) {
        var r = transfer.previewImport(j);
        service.saveImportPreview(
            j.getUserId(), id, r.previewJson(), r.errorJson(), j.getVersion());
      } else if (m.jobType().equals(ContentApplicationService.IMPORT_APPLY)) {
        String e = transfer.applyImport(j);
        service.completeImport(id, e, j.getVersion());
      } else {
        var r = transfer.export(j);
        service.completeExport(id, r.resultFileId(), r.error(), j.getVersion());
      }
    }
    return "{\"status\":\"completed\"}";
  }
}
