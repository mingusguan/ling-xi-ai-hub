package com.lingxi.operations.domain;

import static org.assertj.core.api.Assertions.*;

import com.lingxi.kernel.BusinessException;
import java.time.*;
import org.junit.jupiter.api.Test;

class ConfigReleaseTest {
  @Test
  void productionReleaseRequiresTwoPeopleAndGrayStage() {
    LocalDateTime n = LocalDateTime.of(2026, 8, 5, 1, 0);
    var r = ConfigRelease.create(1, "k", "FEATURE", 1, "ref", "digest", "{}", 10, n);
    r.validated(0, n);
    assertThatThrownBy(() -> r.approve(10, 1, n)).isInstanceOf(BusinessException.class);
    r.approve(11, 1, n);
    r.gray(12, 2, null, n);
    r.publish(12, 3, n);
    assertThat(r.getStatus()).isEqualTo(ConfigRelease.Status.PUBLISHED);
  }
}
