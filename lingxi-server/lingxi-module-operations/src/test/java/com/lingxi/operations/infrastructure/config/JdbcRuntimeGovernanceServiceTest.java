package com.lingxi.operations.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.jdbc.core.JdbcTemplate;

class JdbcRuntimeGovernanceServiceTest {

  private final JdbcRuntimeGovernanceService service =
      new JdbcRuntimeGovernanceService(org.mockito.Mockito.mock(JdbcTemplate.class));

  @Test
  void shouldRejectUnknownClientPlatform() {
    assertThatThrownBy(() -> service.clientBootstrap("ANDROID", 1)).hasMessageContaining("平台不合法");
  }

  @Test
  void shouldRejectNegativeClientVersion() {
    assertThatThrownBy(() -> service.clientBootstrap("HARMONY", -1)).hasMessageContaining("不能为负数");
  }

  @Test
  void shouldResolveEveryStructuredRuntimeResourceThroughPublishedRelease() {
    JdbcTemplate jdbc = Mockito.mock(JdbcTemplate.class);
    new JdbcRuntimeGovernanceService(jdbc).clientBootstrap("PC_WEB", 1);

    List<String> runtimeQueries =
        Mockito.mockingDetails(jdbc).getInvocations().stream()
            .map(invocation -> invocation.getArgument(0, String.class))
            .filter(
                sql ->
                    sql.contains("ops_app_release")
                        || sql.contains("ops_compliance_document")
                        || sql.contains("ops_feature_flag")
                        || sql.contains("ops_experiment"))
            .toList();
    assertThat(runtimeQueries).hasSize(4);
    assertThat(runtimeQueries)
        .allSatisfy(
            sql -> {
              assertThat(sql).contains("JOIN ops_config_release");
              assertThat(sql).contains("cr.status='PUBLISHED'");
            });
  }
}
