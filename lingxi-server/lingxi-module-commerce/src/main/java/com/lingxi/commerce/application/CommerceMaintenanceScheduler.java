package com.lingxi.commerce.application;

import com.lingxi.commerce.domain.CommerceRepository;
import com.lingxi.commerce.domain.Order;
import com.lingxi.commerce.domain.Subscription;
import com.lingxi.kernel.AsyncJobRequest;
import com.lingxi.kernel.AsyncJobScheduler;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** 将订单关闭和订阅对账拆成数据库任务，支持多实例租约执行。 */
@Component
public class CommerceMaintenanceScheduler {
  private final CommerceRepository repository;
  private final AsyncJobScheduler jobs;
  private final Duration orderTimeout;

  public CommerceMaintenanceScheduler(
      CommerceRepository repository,
      AsyncJobScheduler jobs,
      @Value("${lingxi.commerce.order-timeout:PT30M}") Duration orderTimeout) {
    this.repository = repository;
    this.jobs = jobs;
    this.orderTimeout = orderTimeout;
  }

  @Scheduled(cron = "${lingxi.commerce.maintenance-cron:0 */5 * * * *}", zone = "UTC")
  public void scheduleMaintenance() {
    LocalDateTime cutoff = LocalDateTime.now(ZoneOffset.UTC).minus(orderTimeout);
    Instant now = Instant.now();
    List<AsyncJobRequest> requests = new ArrayList<>();
    for (Order order : repository.findClosableOrders(cutoff, 200)) {
      requests.add(
          new AsyncJobRequest(
              CommerceJobHandler.ORDER_CLOSE,
              String.valueOf(order.getId()),
              "{\"orderId\":" + order.getId() + "}",
              5,
              now));
    }
    for (Subscription subscription : repository.findSubscriptionsForReconciliation(200)) {
      requests.add(
          new AsyncJobRequest(
              CommerceJobHandler.SUBSCRIPTION_RECONCILE,
              String.valueOf(subscription.getId()),
              "{\"subscriptionId\":" + subscription.getId() + "}",
              8,
              now));
    }
    jobs.scheduleBatch(requests);
  }
}
