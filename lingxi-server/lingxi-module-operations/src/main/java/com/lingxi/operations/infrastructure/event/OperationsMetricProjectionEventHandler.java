package com.lingxi.operations.infrastructure.event;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lingxi.kernel.BusinessException;
import com.lingxi.kernel.IdGenerator;
import com.lingxi.kernel.PublishedEventHandler;
import com.lingxi.kernel.PublishedEventMessage;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 将核心业务事件投影为日聚合指标。
 *
 * <p>投影只读取事件中的非敏感业务字段；事件消费凭证与指标写入位于同一事务，重试不会重复计数。
 */
@Component
public class OperationsMetricProjectionEventHandler implements PublishedEventHandler {

  private static final Map<String, MetricDefinition> METRICS =
      Map.of(
          "identity.user-registered", new MetricDefinition("user.registered", List.of("ageBand")),
          "goal.created", new MetricDefinition("goal.created", List.of()),
          "goal.plan-activated", new MetricDefinition("goal.plan.activated", List.of()),
          "goal.action-checked-in", new MetricDefinition("goal.action.checked_in", List.of("result")),
          "goal.review-completed", new MetricDefinition("goal.review.completed", List.of()),
          "companion.model-used.v1",new MetricDefinition("ai.model.call",List.of("scene","ageBand","modelVersion")),
          "companion.safety-risk-detected.v1",
              new MetricDefinition("safety.risk.detected", List.of("riskLevel")));

  private final ObjectMapper objectMapper;
  private final JdbcTemplate jdbcTemplate;
  private final IdGenerator idGenerator;

  public OperationsMetricProjectionEventHandler(
      ObjectMapper objectMapper, JdbcTemplate jdbcTemplate, IdGenerator idGenerator) {
    this.objectMapper = objectMapper;
    this.jdbcTemplate = jdbcTemplate;
    this.idGenerator = idGenerator;
  }

  @Override
  public String consumerName() {
    return "operations-daily-metric-projection-v1";
  }

  @Override
  public boolean supports(String eventType) {
    return METRICS.containsKey(eventType);
  }

  @Override
  public void handle(PublishedEventMessage event) {
    MetricDefinition definition = METRICS.get(event.eventType());
    LocalDate metricDate = event.occurredAt().atZone(ZoneOffset.UTC).toLocalDate();
    LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
    increment(metricDate, definition.metricKey(), "ALL", "ALL", now);

    for(String dimensionField:definition.dimensionFields()) {
      String dimensionValue = readDimension(event.payloadJson(), dimensionField);
      increment(
          metricDate,
          definition.metricKey(),
          dimensionField.toUpperCase(),
          dimensionValue,
          now);
    }
    if("companion.model-used.v1".equals(event.eventType()))projectModelUsage(event.payloadJson(),metricDate,now);
  }

  private void projectModelUsage(String payloadJson,LocalDate date,LocalDateTime now){try{JsonNode payload=objectMapper.readTree(payloadJson);add(date,"ai.model.latency_ms","ALL","ALL",payload.path("latencyMillis").asLong(),1,now);add(date,"ai.model.input_tokens","ALL","ALL",payload.path("inputTokens").asLong(),1,now);add(date,"ai.model.output_tokens","ALL","ALL",payload.path("outputTokens").asLong(),1,now);add(date,"ai.model.cost_minor","ALL","ALL",payload.path("costMinor").asLong(),1,now);}catch(Exception exception){throw new BusinessException("OPS_METRIC_EVENT_INVALID","模型计量事件无法解析");}}

  private void add(LocalDate date,String key,String type,String value,long amount,long samples,LocalDateTime now){jdbcTemplate.update("INSERT INTO ops_metric_daily(id,metric_date,metric_key,dimension_type,dimension_value,metric_value,sample_count,created_at,updated_at) VALUES(?,?,?,?,?,?,?,?,?) ON DUPLICATE KEY UPDATE metric_value=metric_value+VALUES(metric_value),sample_count=sample_count+VALUES(sample_count),updated_at=VALUES(updated_at)",idGenerator.nextId(),date,key,type,value,amount,samples,now,now);}

  private String readDimension(String payloadJson, String fieldName) {
    try {
      JsonNode value = objectMapper.readTree(payloadJson).get(fieldName);
      if (value == null || value.isNull() || value.asText().isBlank()) {
        return "UNKNOWN";
      }
      String result = value.asText().trim().toUpperCase();
      return result.length() <= 64 ? result : result.substring(0, 64);
    } catch (Exception exception) {
      throw new BusinessException("OPS_METRIC_EVENT_INVALID", "运营指标事件无法解析");
    }
  }

  private void increment(
      LocalDate metricDate,
      String metricKey,
      String dimensionType,
      String dimensionValue,
      LocalDateTime now) {
    jdbcTemplate.update(
        "INSERT INTO ops_metric_daily(id,metric_date,metric_key,dimension_type,dimension_value,"
            + "metric_value,sample_count,created_at,updated_at) VALUES(?,?,?,?,?,1,1,?,?) "
            + "ON DUPLICATE KEY UPDATE metric_value=metric_value+1,"
            + "sample_count=sample_count+1,updated_at=VALUES(updated_at)",
        idGenerator.nextId(),
        metricDate,
        metricKey,
        dimensionType,
        dimensionValue,
        now,
        now);
  }

  private record MetricDefinition(String metricKey, List<String> dimensionFields) {}
}
