package com.lingxi.content.application;

import com.lingxi.content.domain.TransferJob;

public interface ContentTransferAdapter {
  ParseResult previewImport(TransferJob job) throws Exception;

  ExportResult export(TransferJob job) throws Exception;

  String applyImport(TransferJob job) throws Exception;

  record ParseResult(String previewJson, String errorJson) {}

  record ExportResult(Long resultFileId, String error) {}
}
