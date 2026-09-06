package com.lingxi.identity.application;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.lingxi.identity.api.*;
import com.lingxi.identity.api.AdminIdentityGovernanceFacade.*;
import com.lingxi.identity.domain.IdentityGovernanceRepository;
import com.lingxi.kernel.DomainEventPublisher;
import com.lingxi.kernel.IdGenerator;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class IdentityGovernanceApplicationServiceTest {
  @Mock private IdentityGovernanceRepository repository;
  @Mock private AdminAuthorizationFacade admins;
  @Mock private AuthenticationFacade authentication;
  @Mock private IdGenerator ids;
  @Mock private DomainEventPublisher events;
  private IdentityGovernanceApplicationService service;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    service = new IdentityGovernanceApplicationService(repository, admins, authentication, ids, events);
  }

  @Test
  void shouldRejectUnderFourteenSubmission() {
    var command = new AgeAppealFacade.SubmitAgeAppealCommand(
        1, LocalDate.now().minusYears(13), "private-object:evidence");
    assertThatThrownBy(() -> service.submit(command)).hasMessageContaining("14 岁");
  }

  @Test
  void shouldRejectAdminApprovalWhenClaimedAgeIsUnderFourteen() {
    when(admins.allowed(7, "identity:age-appeal:manage")).thenReturn(true);
    when(repository.findAgeAppeal(9)).thenReturn(new AgeAppealSummary(
        9, "AA9", 1, LocalDate.now().minusYears(13), "private-object:evidence",
        "PENDING", null, null, 0, LocalDateTime.now(), LocalDateTime.now()));
    var command = new AgeAppealReviewCommand(7, 9, true, "证据真实", 0,
        new OperationContext("核验年龄", "TK-2", "REQ-2", true));

    assertThatThrownBy(() -> service.reviewAgeAppeal(command)).hasMessageContaining("14 岁");
  }
}
