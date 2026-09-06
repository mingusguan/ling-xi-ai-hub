package com.lingxi.engagement.infrastructure.event;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lingxi.engagement.domain.EngagementRepository;
import com.lingxi.kernel.IdGenerator;
import com.lingxi.kernel.PublishedEventHandler;
import com.lingxi.kernel.PublishedEventMessage;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * 将 goal 领域事件投影为跨端同步游标（domain=goal），使 PC Web 与 HarmonyOS 能通过
 * /sync/changes 增量感知目标、计划激活、打卡和复盘变更。
 *
 * <p>事件只携带非敏感资源标识；同步载荷不含目标正文或用户笔记。消费凭证与游标写入位于同一
 * 事务，事件重试不会重复投影。
 */
@Component
public class GoalSyncProjectionHandler implements PublishedEventHandler {

  /** 本消费者关心的 goal 领域事件类型。 */
  private static final Set<String> GOAL_EVENTS =
      Set.of(
          "goal.created",
          "goal.plan-activated",
          "goal.action-checked-in",
          "goal.review-completed");

  private final EngagementRepository repository;
  private final IdGenerator ids;
  private final ObjectMapper mapper;

  public GoalSyncProjectionHandler(
      EngagementRepository repository, IdGenerator ids, ObjectMapper mapper) {
    this.repository = repository;
    this.ids = ids;
    this.mapper = mapper;
  }

  @Override
  public String consumerName() {
    return "engagement-goal-sync-projection-v1";
  }

  @Override
  public boolean supports(String eventType) {
    return GOAL_EVENTS.contains(eventType);
  }

  @Override
  public void handle(PublishedEventMessage event) {
    JsonNode payload = parse(event.payloadJson());
    long userId = payload.path("userId").asLong();
    long goalId = payload.path("goalId").asLong();
    if (userId <= 0 || goalId <= 0) {
      // 早期事件未携带 userId 或载荷不完整：无法归属用户，跳过投影而非死信。
      return;
    }
    LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
    repository.insertChange(
        ids.nextId(),
        userId,
        ids.nextId(),
        "goal",
        "goal",
        Long.toString(goalId),
        event.aggregateVersion(),
        operation(event.eventType()),
        snapshot(event.eventType(), goalId, payload),
        now);
  }

  private String operation(String eventType) {
    return switch (eventType) {
      case "goal.created" -> "CREATED";
      case "goal.plan-activated" -> "PLAN_ACTIVATED";
      case "goal.action-checked-in" -> "CHECKED_IN";
      case "goal.review-completed" -> "REVIEW_COMPLETED";
      default -> "UPDATED";
    };
  }

  private String snapshot(String eventType, long goalId, JsonNode payload) {
    try {
      var node = mapper.createObjectNode();
      node.put("goalId", goalId);
      long occurrenceId = payload.path("occurrenceId").asLong();
      if (occurrenceId > 0) node.put("occurrenceId", occurrenceId);
      String result = payload.path("result").asText("");
      if (!result.isBlank()) node.put("result", result);
      return mapper.writeValueAsString(node);
    } catch (Exception e) {
      return "{}";
    }
  }

  private JsonNode parse(String payloadJson) {
    try {
      return mapper.readTree(payloadJson);
    } catch (Exception e) {
      throw new com.lingxi.kernel.BusinessException(
          "ENG_GOAL_EVENT_INVALID", "goal 领域事件载荷无法解析");
    }
  }
}
