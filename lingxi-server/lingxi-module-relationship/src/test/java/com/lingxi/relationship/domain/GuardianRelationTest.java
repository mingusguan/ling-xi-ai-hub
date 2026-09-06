package com.lingxi.relationship.domain;

import static org.assertj.core.api.Assertions.*;

import com.lingxi.kernel.BusinessException;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class GuardianRelationTest {
  @Test
  void shouldAcceptValidInvitationAndRejectSelfBinding() {
    LocalDateTime now = LocalDateTime.of(2026, 8, 5, 10, 0);
    GuardianRelation valid =
        GuardianRelation.invite(1, "key", "digest", 14, "token", "[]", now.plusDays(1), now);
    valid.accept(18, now.plusMinutes(1));
    assertThat(valid.getStatus()).isEqualTo(GuardianRelationStatus.ACTIVE);
    GuardianRelation self =
        GuardianRelation.invite(2, "key2", "digest", 14, "token2", "[]", now.plusDays(1), now);
    assertThatThrownBy(() -> self.accept(14, now))
        .isInstanceOf(BusinessException.class)
        .extracting("code")
        .isEqualTo("GUARDIAN_SELF_BINDING");
  }

  @Test
  void shouldRejectExpiredInvitation() {
    LocalDateTime now = LocalDateTime.of(2026, 8, 5, 10, 0);
    GuardianRelation relation =
        GuardianRelation.invite(
            1, "key", "digest", 14, "token", "[]", now.minusSeconds(1), now.minusDays(1));
    assertThatThrownBy(() -> relation.accept(18, now))
        .isInstanceOf(BusinessException.class)
        .extracting("code")
        .isEqualTo("GUARDIAN_INVITATION_INVALID");
  }
}
