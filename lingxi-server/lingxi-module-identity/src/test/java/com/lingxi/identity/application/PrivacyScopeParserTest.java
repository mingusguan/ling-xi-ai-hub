package com.lingxi.identity.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lingxi.identity.api.PrivacyRequestType;
import com.lingxi.kernel.BusinessException;
import org.junit.jupiter.api.Test;

class PrivacyScopeParserTest {
  private final PrivacyScopeParser parser = new PrivacyScopeParser(new ObjectMapper());

  @Test
  void emptyObjectMeansAllModulesAndCanonicalizesStably() {
    var scope = parser.parse("{}", PrivacyRequestType.EXPORT);
    assertThat(scope.isAllModules()).isTrue();
    assertThat(parser.canonicalJson(scope)).isEqualTo("{}");
  }

  @Test
  void explicitEmptyWhitelistIsRejected() {
    assertThatThrownBy(() -> parser.parse("{\"modules\":[]}", PrivacyRequestType.EXPORT))
        .isInstanceOfSatisfying(
            BusinessException.class,
            error -> assertThat(error.getCode()).isEqualTo("PRIVACY_INVALID_SCOPE"));
  }

  @Test
  void accountClosureCannotNarrowItsCoverage() {
    assertThatThrownBy(
            () -> parser.parse("{\"modules\":[\"identity\"]}", PrivacyRequestType.CLOSE_ACCOUNT))
        .isInstanceOf(BusinessException.class);
  }
}
