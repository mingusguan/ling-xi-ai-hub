package com.lingxi.content.application;

import com.lingxi.content.domain.FileAsset;

public interface ContentScanAdapter {
  ScanVerdict scan(FileAsset file) throws Exception;

  record ScanVerdict(boolean safe, String detail) {}
}
