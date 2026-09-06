package com.lingxi.operations.infrastructure.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lingxi.identity.api.AdminManagementAuditedEvent;
import com.lingxi.identity.api.AdminActionAuditedEvent;
import com.lingxi.kernel.*;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** identity 管理审计事件的幂等投影消费者。 */
@Component
public class AdminManagementAuditEventHandler implements PublishedEventHandler {
  private final ObjectMapper json; private final JdbcTemplate jdbc; private final IdGenerator ids;
  public AdminManagementAuditEventHandler(ObjectMapper json,JdbcTemplate jdbc,IdGenerator ids){this.json=json;this.jdbc=jdbc;this.ids=ids;}
  public String consumerName(){return "operations-admin-management-audit-v1";}
  public boolean supports(String eventType){return "identity.admin-management-audited.v1".equals(eventType)||"admin-action-audited.v1".equals(eventType);}
  public void handle(PublishedEventMessage message){
    AuditProjection e;
    try{
      if("admin-action-audited.v1".equals(message.eventType())){
        AdminActionAuditedEvent value=json.readValue(message.payloadJson(),AdminActionAuditedEvent.class);
        e=new AuditProjection(value.eventId(),value.operatorAdminId(),value.action(),value.objectType(),value.objectId(),value.reason(),value.ticketNo(),value.occurredAt());
      }else{
        AdminManagementAuditedEvent value=json.readValue(message.payloadJson(),AdminManagementAuditedEvent.class);
        e=new AuditProjection(value.eventId(),value.operatorAdminId(),value.action(),value.objectType(),value.objectId(),value.reason(),value.ticketNo(),value.occurredAt());
      }
    }catch(Exception exception){throw new BusinessException("OPS_ADMIN_AUDIT_EVENT_INVALID","管理员审计事件无法解析");}
    jdbc.update("INSERT IGNORE INTO ops_audit_log(id,admin_id,action,object_type,object_id,reason,ticket_no,result,request_id,created_at,deleted,source_event_id) VALUES(?,?,?,?,?,?,?,'SUCCESS',?,?,0,?)",
        ids.nextId(),e.operatorAdminId(),e.action(),e.objectType(),e.objectId(),e.reason(),e.ticketNo(),e.eventId(),LocalDateTime.ofInstant(e.occurredAt(),ZoneOffset.UTC),e.eventId());
  }
  private record AuditProjection(String eventId,long operatorAdminId,String action,String objectType,String objectId,String reason,String ticketNo,java.time.Instant occurredAt){}
}
