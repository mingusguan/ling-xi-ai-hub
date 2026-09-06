package com.lingxi.identity.application;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.lingxi.identity.api.*;
import com.lingxi.identity.infrastructure.persistence.UserEntity;
import com.lingxi.identity.infrastructure.persistence.UserMapper;
import com.lingxi.kernel.DomainEventPublisher;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class IdentityAdminApplicationServiceTest {
  @Test
  void shouldRejectTeenRestoreWithoutActiveGuardian() {
    UserMapper users=Mockito.mock(UserMapper.class);
    AdminAuthorizationFacade admins=Mockito.mock(AdminAuthorizationFacade.class);
    when(admins.allowed(7,"identity:user:restrict")).thenReturn(true);
    UserEntity user=new UserEntity();user.setId(9L);user.setAgeBand("TEEN");user.setStatus("RESTRICTED");
    user.setVersion(3L);user.setAuthorizationVersion(5L);user.setPublicId("U9");
    when(users.selectById(9L)).thenReturn(user);
    IdentityAdminApplicationService service=new IdentityAdminApplicationService(users,admins,
        Mockito.mock(AuthenticationFacade.class),Mockito.mock(DomainEventPublisher.class),
        List.of(teenUserId->false));
    var command=new AdminUserFacade.AdminUserStatusCommand(7,9,"ACTIVE_TEEN","解限","TK-1",3,true);
    assertThatThrownBy(()->service.changeStatus(command)).hasMessageContaining("有效监护关系");
  }
}
