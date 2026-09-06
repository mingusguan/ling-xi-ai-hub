package com.lingxi.identity.application;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import com.lingxi.identity.api.PrivacyContribution;
import com.lingxi.identity.api.PrivacyProcessingContext;
import com.lingxi.identity.api.PrivacyDataContributor;
import com.lingxi.identity.api.PrivacyRequestType;
import com.lingxi.identity.api.PrivacyScope;
import com.lingxi.kernel.BusinessException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class PrivacyRequestJobHandlerTest {
  @Test
  void refusesToCompleteExportWhenBusinessModulesAreMissing() {
    PrivacyDataContributor identity = contributor("identity");
    PrivacyDataContributor goal = contributor("goal");
    PrivacyRequestJobHandler handler = handler(List.of(identity, goal));

    PrivacyScope allModules = new PrivacyScope(Set.of());
    assertThatThrownBy(
            () ->
                handler.requireCompleteCoverage(
                    PrivacyRequestType.EXPORT, allModules, List.of(identity, goal)))
        .isInstanceOf(BusinessException.class)
        .extracting("code")
        .isEqualTo("PRIVACY_COVERAGE_INCOMPLETE");
    PrivacyDataContributor operations = contributor("operations");
    assertThatCode(
            () ->
                handler.requireCompleteCoverage(
                    PrivacyRequestType.CORRECTION,
                    allModules,
                    List.of(operations)))
        .doesNotThrowAnyException();
  }

  private PrivacyRequestJobHandler handler(List<PrivacyDataContributor> contributors) {
    ObjectMapper objectMapper = new ObjectMapper();
    return new PrivacyRequestJobHandler(
        null,
        null,
        contributors,
        objectMapper,
        null,
        new PrivacyScopeParser(objectMapper),
        null,
        null,
        List.of());
  }

  private PrivacyDataContributor contributor(String moduleName) {
    return new PrivacyDataContributor() {
      @Override
      public String moduleName() {
        return moduleName;
      }

      @Override
      public PrivacyContribution process(PrivacyProcessingContext context) {
        return PrivacyContribution.unchanged();
      }
    };
  }
}
