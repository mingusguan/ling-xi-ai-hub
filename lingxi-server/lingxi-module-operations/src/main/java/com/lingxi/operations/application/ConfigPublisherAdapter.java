package com.lingxi.operations.application;

import com.lingxi.operations.domain.ConfigRelease;

public interface ConfigPublisherAdapter {
  ValidationResult validate(ConfigRelease release);

  void startGray(ConfigRelease release);

  void publish(ConfigRelease release);

  void rollback(ConfigRelease release);

  record ValidationResult(boolean valid, String reason) {}
}
