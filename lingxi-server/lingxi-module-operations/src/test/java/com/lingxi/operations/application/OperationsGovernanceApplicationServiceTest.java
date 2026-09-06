package com.lingxi.operations.application;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lingxi.kernel.DomainEventPublisher;
import com.lingxi.kernel.IdGenerator;
import com.lingxi.operations.api.AuditContext;
import com.lingxi.operations.api.OperationsGovernanceFacade.CampaignCommand;
import com.lingxi.operations.api.OperationsGovernanceFacade.RuntimeStatusCommand;
import com.lingxi.operations.domain.OperationsGovernanceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class OperationsGovernanceApplicationServiceTest {
  @Mock private OperationsGovernanceRepository repository;
  @Mock private AdminPermissionAdapter permissions;
  @Mock private IdGenerator ids;
  @Mock private DomainEventPublisher events;
  private OperationsGovernanceApplicationService service;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    service =
        new OperationsGovernanceApplicationService(
            repository, permissions, ids, events, new ObjectMapper());
    when(permissions.allowed(7, "message:campaign:manage")).thenReturn(true);
  }

  @Test
  void shouldNeverAllowTeenMarketingCampaign() {
    CampaignCommand command =
        new CampaignCommand(
            0,
            "campaign",
            "活动",
            "PUSH",
            "{}",
            "内容",
            "{}",
            true,
            null,
            "DRAFT",
            0,
            new AuditContext("运营活动", "TK-1", "REQ-1", true));

    assertThatThrownBy(() -> service.saveCampaign(7, command)).hasMessageContaining("14-17 岁");
  }

  @Test
  void shouldRejectTeenHiddenInAudienceRule() {
    CampaignCommand command =
        new CampaignCommand(
            0,
            "campaign",
            "活动",
            "PUSH",
            "{\"segment\":\"MINOR_USERS\"}",
            "内容",
            "{}",
            false,
            null,
            "DRAFT",
            0,
            new AuditContext("运营活动", "TK-1", "REQ-1", true));
    assertThatThrownBy(() -> service.saveCampaign(7, command)).hasMessageContaining("14-17 岁");
  }

  @Test
  void shouldRequireReleaseWorkflowForRunningResource() {
    CampaignCommand command =
        new CampaignCommand(
            0,
            "campaign",
            "活动",
            "PUSH",
            "{}",
            "内容",
            "{}",
            false,
            null,
            "RUNNING",
            0,
            new AuditContext("运营活动", "TK-1", "REQ-1", true));
    assertThatThrownBy(() -> service.saveCampaign(7, command)).hasMessageContaining("发布流程");
  }

  @Test
  void shouldNeverDisableMandatoryFeatureFlag() {
    when(permissions.allowed(7, "feature:flag:manage")).thenReturn(true);
    when(repository.findResource("FEATURE_FLAG", 9))
        .thenReturn(new OperationsGovernanceRepository.ResourceState(9, "ACTIVE", true, 3L, 2));
    RuntimeStatusCommand command =
        new RuntimeStatusCommand(
            "FEATURE_FLAG", 9, 3, "DISABLED", 2, new AuditContext("安全策略", "TK-1", "REQ-1", true));
    assertThatThrownBy(() -> service.changeRuntimeStatus(7, command)).hasMessageContaining("不可关闭");
  }

  @Test
  void shouldRejectReverseRuntimeTransition() {
    when(repository.findResource("CAMPAIGN", 9))
        .thenReturn(
            new OperationsGovernanceRepository.ResourceState(9, "COMPLETED", false, null, 2));
    RuntimeStatusCommand command =
        new RuntimeStatusCommand(
            "CAMPAIGN", 9, 3, "RUNNING", 2, new AuditContext("恢复活动", "TK-2", "REQ-2", true));

    assertThatThrownBy(() -> service.changeRuntimeStatus(7, command))
        .hasMessageContaining("不可按当前路径流转");
  }

  @Test
  void shouldBindPublishedReleaseToExactGovernanceResource() {
    when(repository.findResource("CAMPAIGN", 9))
        .thenReturn(new OperationsGovernanceRepository.ResourceState(9, "DRAFT", false, null, 2));
    RuntimeStatusCommand command =
        new RuntimeStatusCommand(
            "CAMPAIGN", 9, 3, "RUNNING", 2, new AuditContext("启动活动", "TK-3", "REQ-3", true));

    assertThatThrownBy(() -> service.changeRuntimeStatus(7, command))
        .hasMessageContaining("内容引用匹配");
    verify(repository).isPublishedRelease(3, "MESSAGE_CAMPAIGN", "governance://CAMPAIGN/9");
  }
}
