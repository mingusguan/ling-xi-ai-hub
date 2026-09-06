package com.lingxi.commerce.application;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lingxi.commerce.api.AdminCommerceManagementFacade.OperationContext;
import com.lingxi.commerce.api.AdminCommerceManagementFacade.ProductCommand;
import com.lingxi.commerce.api.AdminCommerceManagementFacade.PromotionCommand;
import com.lingxi.commerce.domain.CommerceAdminWriteRepository;
import com.lingxi.commerce.domain.CommerceRepository;
import com.lingxi.identity.api.AdminAuthorizationFacade;
import com.lingxi.kernel.DomainEventPublisher;
import com.lingxi.kernel.IdGenerator;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class CommerceAdminManagementApplicationServiceTest {
  private CommerceAdminManagementApplicationService service;

  @BeforeEach
  void setUp() {
    CommerceAdminWriteRepository writes = Mockito.mock(CommerceAdminWriteRepository.class);
    CommerceRepository commerce = Mockito.mock(CommerceRepository.class);
    CommerceTransactionService transactions = Mockito.mock(CommerceTransactionService.class);
    AdminAuthorizationFacade admins = Mockito.mock(AdminAuthorizationFacade.class);
    when(admins.allowed(7, "commerce:catalog:manage")).thenReturn(true);
    service =
        new CommerceAdminManagementApplicationService(
            writes,
            commerce,
            transactions,
            List.of(),
            admins,
            Mockito.mock(IdGenerator.class),
            Mockito.mock(DomainEventPublisher.class),
            new ObjectMapper());
  }

  @Test
  void shouldRejectRecurringProductForTeenAudience() {
    ProductCommand command =
        new ProductCommand(
            0,
            "MONTHLY_TEEN",
            "月度会员",
            "ALL",
            "MONTHLY",
            "TEEN_ALLOWED",
            "MEMBER",
            1,
            "DRAFT",
            0,
            context());
    assertThatThrownBy(() -> service.saveProduct(7, command)).hasMessageContaining("不得购买或自动续费");
  }

  @Test
  void shouldRejectSensitiveOrTeenPromotionAudience() {
    PromotionCommand command =
        new PromotionCommand(
            0,
            "P1",
            "促销",
            "DISCOUNT",
            "{}",
            "{\"segment\":\"MINOR_USERS\"}",
            LocalDateTime.now(),
            LocalDateTime.now().plusDays(1),
            "DRAFT",
            0,
            context());
    assertThatThrownBy(() -> service.savePromotion(7, command)).hasMessageContaining("14-17 岁");
  }

  private OperationContext context() {
    return new OperationContext("测试", "TK-1", "REQ-1", true);
  }
}
