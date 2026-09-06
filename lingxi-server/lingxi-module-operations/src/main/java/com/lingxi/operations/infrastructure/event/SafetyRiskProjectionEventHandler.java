package com.lingxi.operations.infrastructure.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lingxi.companion.api.SafetyRiskDetectedEvent;
import com.lingxi.kernel.BusinessException;
import com.lingxi.kernel.IdGenerator;
import com.lingxi.identity.api.GuardianNotificationProvider;
import com.lingxi.kernel.PublishedEventHandler;
import com.lingxi.kernel.PublishedEventMessage;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import java.util.List;

/** 将 Agent 安全命中投影为后台人工处置事件，并为严重事件生成值班告警。 */
@Component
public class SafetyRiskProjectionEventHandler implements PublishedEventHandler {
  private final ObjectMapper json;
  private final JdbcTemplate jdbc;
  private final IdGenerator ids;
  private final GuardianNotificationProvider guardians;

  public SafetyRiskProjectionEventHandler(ObjectMapper json, JdbcTemplate jdbc, IdGenerator ids,
      List<GuardianNotificationProvider> guardianProviders) {
    this.json = json;
    this.jdbc = jdbc;
    this.ids = ids;
    this.guardians=guardianProviders.stream().findFirst().orElse(userId->List.of());
  }

  @Override public String consumerName() { return "operations-safety-risk-projection-v1"; }
  @Override public boolean supports(String eventType) {
    return "companion.safety-risk-detected.v1".equals(eventType);
  }

  @Override
  public void handle(PublishedEventMessage message) {
    SafetyRiskDetectedEvent event;
    try {
      event = json.readValue(message.payloadJson(), SafetyRiskDetectedEvent.class);
    } catch (Exception exception) {
      throw new BusinessException("OPS_SAFETY_EVENT_INVALID", "安全事件无法解析");
    }
    LocalDateTime occurredAt = LocalDateTime.ofInstant(event.occurredAt(), ZoneOffset.UTC);
    long caseId = ids.nextId();
    int inserted = jdbc.update(
        "INSERT IGNORE INTO ops_safety_case(id,case_no,user_id,source_type,source_id,risk_category,risk_level,content_excerpt,status,version,deleted,created_at,updated_at) VALUES(?,?,?,?,?,?,?,?,'OPEN',0,0,?,?)",
        caseId, "SC" + event.safetyEventId(), event.userId(), "AGENT_SAFETY",
        String.valueOf(event.safetyEventId()), event.riskType(), event.riskLevel(),
        "披露范围：" + event.disclosureScope(), occurredAt, occurredAt);
    if (inserted == 1 && ("HIGH".equals(event.riskLevel()) || "CRITICAL".equals(event.riskLevel()))) {
      jdbc.update(
          "INSERT IGNORE INTO ops_safety_alert(id,safety_case_id,alert_channel,recipient_ref,status,created_at,updated_at) VALUES(?,?,'DUTY_QUEUE','SAFETY_ON_CALL','PENDING',?,?)",
          ids.nextId(), caseId, occurredAt, occurredAt);
      // 监护通知只包含安全事件引用，不披露对话正文、风险原文或其他隐私内容。
      for(Long guardianId:guardians.activeGuardianUserIds(event.userId())){
        jdbc.update("INSERT IGNORE INTO ops_safety_alert(id,safety_case_id,alert_channel,recipient_ref,status,created_at,updated_at) VALUES(?,?,'GUARDIAN_INBOX',?,'PENDING',?,?)",
            ids.nextId(),caseId,"USER:"+guardianId,occurredAt,occurredAt);
      }
    }
  }
}
