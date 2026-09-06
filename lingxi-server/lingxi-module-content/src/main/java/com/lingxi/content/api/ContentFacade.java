package com.lingxi.content.api;

import java.util.List;

/** 私有文件、模板和导入导出的公开门面。 */
public interface ContentFacade {
  UploadTicketResult createUploadTicket(CreateUploadTicketCommand command);

  FileResult markUploaded(long userId, long fileId, long expectedVersion);

  FileResult completeScan(long fileId, boolean safe, String detail, long expectedVersion);

  FileResult referenceReadyFile(ReferenceFileCommand command);

  FileResult getReadyFile(long userId, long fileId);

  FileResult removeReference(long userId, long fileId, String resourceType, String resourceId);

  FileResult deleteFile(long userId, long fileId, long expectedVersion);

  TransferJobResult createImport(CreateImportJobCommand command);

  TransferJobResult saveImportPreview(
      long userId, long jobId, String previewJson, String errorJson, long expectedVersion);

  TransferJobResult confirmImport(long userId, long jobId, long expectedVersion);

  TransferJobResult createExport(CreateExportJobCommand command);

  TransferJobResult completeExport(
      long jobId, Long resultFileId, String error, long expectedVersion);

  TransferJobResult getTransfer(long userId, long jobId);

  TemplateVersionResult createTemplate(CreateTemplateVersionCommand command);

  TemplateVersionResult submitTemplate(long operatorId, long versionId, long expectedVersion);

  TemplateVersionResult reviewTemplate(
      TemplateReviewCommand command);

  TemplateVersionResult retireTemplate(TemplateRetireCommand command);

  List<TemplateVersionResult> listAdminTemplates(long operatorId, String status);

  List<TemplateVersionResult> listPublishedTemplates(long userId);

  record TemplateReviewCommand(long reviewerId, long versionId, boolean approved,
      String reviewReason, long expectedVersion, boolean recentAuthentication,
      String operationReason, String ticketNo) {}

  record TemplateRetireCommand(long operatorId, long versionId, long expectedVersion,
      boolean recentAuthentication, String operationReason, String ticketNo) {}
}
