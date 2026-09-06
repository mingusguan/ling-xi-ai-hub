package com.lingxi.content.domain;

import static org.assertj.core.api.Assertions.*;

import com.lingxi.kernel.BusinessException;
import java.time.*;
import org.junit.jupiter.api.Test;

class ContentDomainTest {
  @Test
  void templateRejectsUnknownAgeScope() {
    LocalDateTime now = LocalDateTime.of(2026, 8, 5, 1, 0);
    assertThatThrownBy(() -> TemplateVersion.create(1, 2, 1, "CHILD", "{}", now))
        .isInstanceOf(BusinessException.class)
        .extracting("code")
        .isEqualTo("CONTENT_INVALID_TEMPLATE");
  }

  @Test
  void fileCannotBeReferencedBeforeSafeScan() {
    LocalDateTime n = LocalDateTime.of(2026, 8, 5, 1, 0);
    var f =
        FileAsset.create(
            1,
            "p",
            "k",
            2,
            "EVIDENCE",
            "a.png",
            "private/2/p",
            "abc",
            10,
            "image/png",
            "PRIVATE",
            n);
    assertThatThrownBy(() -> f.assertReady(2)).isInstanceOf(BusinessException.class);
    f.uploaded(2, 0, n);
    f.scanning(1, n);
    f.scanned(true, "clean", 2, n);
    assertThatCode(() -> f.assertReady(2)).doesNotThrowAnyException();
  }

  @Test
  void importRequiresPreviewBeforeConfirm() {
    LocalDateTime n = LocalDateTime.of(2026, 8, 5, 1, 0);
    var j = TransferJob.importJob(1, "k", 2, 3, "CSV", n);
    assertThatThrownBy(() -> j.confirm(2, 0, n)).isInstanceOf(BusinessException.class);
    j.preview(2, "{}", null, 0, n);
    j.confirm(2, 1, n);
    assertThat(j.getStatus()).isEqualTo(TransferJob.Status.CONFIRMED);
  }

  @Test
  void publishedTemplateIsImmutableState() {
    LocalDateTime n = LocalDateTime.of(2026, 8, 5, 1, 0);
    var t = TemplateVersion.create(1, 2, 1, "TEEN", "{}", n);
    t.submit(0, n);
    t.review(9, true, "ok", 1, n);
    assertThat(t.getStatus()).isEqualTo(TemplateVersion.Status.PUBLISHED);
    assertThatThrownBy(() -> t.submit(2, n)).isInstanceOf(BusinessException.class);
  }

  @Test
  void fileDeletionRequiresOwnerAndMovesThroughCleanupState() {
    LocalDateTime now = LocalDateTime.of(2026, 8, 6, 1, 0);
    FileAsset file =
        FileAsset.create(
            1,
            "p",
            "k",
            2,
            "EVIDENCE",
            "a.png",
            "private/2/p",
            "abc",
            10,
            "image/png",
            "PRIVATE",
            now);
    assertThatThrownBy(() -> file.beginDeletion(3, 0, now)).isInstanceOf(BusinessException.class);
    file.beginDeletion(2, 0, now);
    assertThat(file.getStatus()).isEqualTo(FileAsset.Status.DELETING);
    file.completeDeletion(1, now);
    assertThat(file.getStatus()).isEqualTo(FileAsset.Status.DELETED);
    assertThatThrownBy(() -> file.assertReady(2)).isInstanceOf(BusinessException.class);
  }
}
