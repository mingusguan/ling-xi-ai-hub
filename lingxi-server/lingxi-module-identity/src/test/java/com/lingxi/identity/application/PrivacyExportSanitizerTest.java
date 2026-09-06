package com.lingxi.identity.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class PrivacyExportSanitizerTest {
  private final ObjectMapper objectMapper = new ObjectMapper();
  private final PrivacyExportSanitizer sanitizer = new PrivacyExportSanitizer();

  @Test
  void stripsCredentialAndUnexpectedFieldsAtEveryRecordBoundary() throws Exception {
    var source =
        objectMapper.readTree(
            "{\"calendars\":[{\"id\":1,\"provider\":\"harmony\","
                + "\"credentialReference\":\"secret\",\"newInternalField\":\"secret2\"}],"
                + "\"unknownRoot\":[1]}");

    var sanitized = sanitizer.sanitize("engagement", source);

    assertThat(sanitized.has("unknownRoot")).isFalse();
    assertThat(sanitized.path("calendars").get(0).has("credentialReference")).isFalse();
    assertThat(sanitized.path("calendars").get(0).has("newInternalField")).isFalse();
    assertThat(sanitized.path("calendars").get(0).path("provider").asText())
        .isEqualTo("harmony");
  }
}
