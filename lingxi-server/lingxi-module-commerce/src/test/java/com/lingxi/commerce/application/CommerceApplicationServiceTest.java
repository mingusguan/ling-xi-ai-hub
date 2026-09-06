package com.lingxi.commerce.application;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.lingxi.commerce.api.ConfirmRefundCommand;
import com.lingxi.commerce.api.CreateOrderCommand;
import com.lingxi.commerce.domain.CommerceRepository;
import com.lingxi.commerce.domain.Order;
import com.lingxi.identity.api.IdentityFacade;
import com.lingxi.kernel.IdGenerator;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class CommerceApplicationServiceTest {

  @Test
  void shouldReturnExistingIdempotentOrderWithoutReadingCurrentCatalog() {
    CommerceRepository repository = Mockito.mock(CommerceRepository.class);
    IdentityFacade identities = Mockito.mock(IdentityFacade.class);
    CommerceTransactionService transactions = Mockito.mock(CommerceTransactionService.class);
    Order order = order(Order.Status.PAYING);
    when(repository.findOrderByBusinessKey("biz-1")).thenReturn(Optional.of(order));
    CommerceApplicationService service =
        new CommerceApplicationService(
            repository,
            identities,
            Mockito.mock(IdGenerator.class),
            List.of(),
            List.of(),
            transactions,
            List.of());

    service.createOrder(new CreateOrderCommand("biz-1", 8, 10, 2, "WECHAT", null));

    verify(repository, never()).findPrice(anyLong(), anyInt());
    verify(identities, never()).getAccessProfile(anyLong());
  }

  @Test
  void shouldReturnExistingRefundWithoutCallingExternalPolicy() {
    CommerceRepository repository = Mockito.mock(CommerceRepository.class);
    IdentityFacade identities = Mockito.mock(IdentityFacade.class);
    CommerceTransactionService transactions = Mockito.mock(CommerceTransactionService.class);
    RefundPolicyAdapter policy = Mockito.mock(RefundPolicyAdapter.class);
    Order order = order(Order.Status.REFUNDED);
    ConfirmRefundCommand command =
        new ConfirmRefundCommand("WECHAT", "refund-1", "LX-1", 1999, "retry");
    when(policy.channel()).thenReturn("WECHAT");
    when(repository.findOrderByNo("LX-1")).thenReturn(Optional.of(order));
    when(repository.findRefund("WECHAT", "refund-1"))
        .thenReturn(Optional.of(new CommerceRepository.RefundSnapshot(1, 1999)));
    CommerceApplicationService service =
        new CommerceApplicationService(
            repository,
            identities,
            Mockito.mock(IdGenerator.class),
            List.of(),
            List.of(),
            transactions,
            List.of(policy));

    service.confirmRefund(command);

    verify(repository, never()).findOrderPriceSnapshot(anyLong());
    verify(policy, never()).evaluate(Mockito.any(), Mockito.any(), Mockito.any());
    verify(transactions, never()).applyRefund(Mockito.any(), anyLong());
  }

  private Order order(Order.Status status) {
    LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
    return Order.rehydrate(
        1, "LX-1", "biz-1", 8, 10, 20, 2, 1999, "CNY", "WECHAT", status, "pay-ref", 0, 1, now, now);
  }
}
