package com.lingxi.operations.infrastructure.persistence;

import com.lingxi.kernel.BusinessException;
import com.lingxi.operations.api.OperationsGovernanceFacade.*;
import com.lingxi.operations.domain.OperationsGovernanceRepository;
import java.time.LocalDateTime;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** 各治理资源独立表的 JDBC 仓储，不以通用配置表替代业务事实。 */
@Repository
public class JdbcOperationsGovernanceRepository implements OperationsGovernanceRepository {
  private static final Map<String,String> RESOURCE_TABLES=Map.of(
      "CAMPAIGN","ops_message_campaign","APP_RELEASE","ops_app_release",
      "COMPLIANCE","ops_compliance_document","FEATURE_FLAG","ops_feature_flag",
      "EXPERIMENT","ops_experiment");
  private final JdbcTemplate jdbc;
  public JdbcOperationsGovernanceRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

  @Override
  public ManagedResource saveCampaign(long id, CampaignCommand c, LocalDateTime now) {
    long next = c.id() == 0 ? 0 : c.expectedVersion() + 1;
    if (c.id() == 0) {
      jdbc.update("INSERT INTO ops_message_campaign(id,campaign_key,name,channel,audience_rule,template_content,frequency_rule,teen_marketing_enabled,scheduled_at,status,version,deleted,created_at,updated_at) VALUES(?,?,?,?,?,?,?,?,?,?,0,0,?,?)",
          id,c.campaignKey(),c.name(),c.channel(),c.audienceRule(),c.templateContent(),
          c.frequencyRule(),c.teenMarketingEnabled(),c.scheduledAt(),c.status(),now,now);
    } else requireUpdated(jdbc.update("UPDATE ops_message_campaign SET name=?,channel=?,audience_rule=?,template_content=?,frequency_rule=?,teen_marketing_enabled=?,scheduled_at=?,status=?,version=?,updated_at=? WHERE id=? AND version=? AND deleted=0",
        c.name(),c.channel(),c.audienceRule(),c.templateContent(),c.frequencyRule(),
        c.teenMarketingEnabled(),c.scheduledAt(),c.status(),next,now,c.id(),c.expectedVersion()));
    return new ManagedResource(id,"CAMPAIGN",c.campaignKey(),c.status(),next,now);
  }

  @Override
  public ManagedResource saveAppRelease(long id, AppReleaseCommand c, LocalDateTime now) {
    long next=c.id()==0?0:c.expectedVersion()+1;
    if(c.id()==0) jdbc.update("INSERT INTO ops_app_release(id,platform,version_name,version_code,minimum_version_code,force_upgrade,gray_rule,release_notes,status,version,deleted,created_at,updated_at) VALUES(?,?,?,?,?,?,?,?,?,0,0,?,?)",
        id,c.platform(),c.versionName(),c.versionCode(),c.minimumVersionCode(),c.forceUpgrade(),c.grayRule(),c.releaseNotes(),c.status(),now,now);
    else requireUpdated(jdbc.update("UPDATE ops_app_release SET version_name=?,minimum_version_code=?,force_upgrade=?,gray_rule=?,release_notes=?,status=?,version=?,updated_at=? WHERE id=? AND version=? AND deleted=0",
        c.versionName(),c.minimumVersionCode(),c.forceUpgrade(),c.grayRule(),c.releaseNotes(),c.status(),next,now,c.id(),c.expectedVersion()));
    return new ManagedResource(id,"APP_RELEASE",c.platform()+":"+c.versionCode(),c.status(),next,now);
  }

  @Override
  public ManagedResource saveComplianceDocument(long id, ComplianceDocumentCommand c, LocalDateTime now) {
    long next=c.id()==0?0:c.expectedVersion()+1;
    if(c.id()==0) jdbc.update("INSERT INTO ops_compliance_document(id,document_type,version_no,title,content_ref,content_digest,effective_at,status,version,deleted,created_at,updated_at) VALUES(?,?,?,?,?,?,?,?,0,0,?,?)",
        id,c.documentType(),c.versionNo(),c.title(),c.contentRef(),c.contentDigest(),c.effectiveAt(),c.status(),now,now);
    else requireUpdated(jdbc.update("UPDATE ops_compliance_document SET title=?,content_ref=?,content_digest=?,effective_at=?,status=?,version=?,updated_at=? WHERE id=? AND version=? AND deleted=0",
        c.title(),c.contentRef(),c.contentDigest(),c.effectiveAt(),c.status(),next,now,c.id(),c.expectedVersion()));
    return new ManagedResource(id,"COMPLIANCE",c.documentType()+":"+c.versionNo(),c.status(),next,now);
  }

  @Override
  public ManagedResource saveFeatureFlag(long id, FeatureFlagCommand c, LocalDateTime now) {
    long next=c.id()==0?0:c.expectedVersion()+1;
    if(c.id()==0) jdbc.update("INSERT INTO ops_feature_flag(id,flag_key,current_release_id,mandatory_policy,status,version,deleted,updated_at) VALUES(?,?,?,?,?,0,0,?)",
        id,c.flagKey(),c.currentReleaseId(),c.mandatoryPolicy(),c.status(),now);
    else requireUpdated(jdbc.update("UPDATE ops_feature_flag SET current_release_id=?,mandatory_policy=?,status=?,version=?,updated_at=? WHERE id=? AND version=? AND deleted=0",
        c.currentReleaseId(),c.mandatoryPolicy(),c.status(),next,now,c.id(),c.expectedVersion()));
    return new ManagedResource(id,"FEATURE_FLAG",c.flagKey(),c.status(),next,now);
  }

  @Override
  public ManagedResource saveExperiment(long id, ExperimentCommand c, LocalDateTime now) {
    long next=c.id()==0?0:c.expectedVersion()+1;
    if(c.id()==0) jdbc.update("INSERT INTO ops_experiment(id,experiment_key,hypothesis,audience_rule,metrics_json,status,current_release_id,version,deleted,created_at,updated_at) VALUES(?,?,?,?,?,?,?,0,0,?,?)",
        id,c.experimentKey(),c.hypothesis(),c.audienceRule(),c.metricsJson(),c.status(),c.currentReleaseId(),now,now);
    else requireUpdated(jdbc.update("UPDATE ops_experiment SET hypothesis=?,audience_rule=?,metrics_json=?,status=?,current_release_id=?,version=?,updated_at=? WHERE id=? AND version=? AND deleted=0",
        c.hypothesis(),c.audienceRule(),c.metricsJson(),c.status(),c.currentReleaseId(),next,now,c.id(),c.expectedVersion()));
    return new ManagedResource(id,"EXPERIMENT",c.experimentKey(),c.status(),next,now);
  }

  @Override
  public ResourceState findResource(String type,long id){
    String table=table(type);String mandatory="FEATURE_FLAG".equals(type)?"mandatory_policy":"0";
    return jdbc.query("SELECT id,status,"+mandatory+" AS mandatory_policy,current_release_id,version FROM "+table+" WHERE id=? AND deleted=0",
        (rs,row)->new ResourceState(rs.getLong("id"),rs.getString("status"),rs.getBoolean("mandatory_policy"),nullableLong(rs,"current_release_id"),rs.getLong("version")),id).stream().findFirst().orElse(null);
  }

  @Override
  public boolean isPublishedRelease(long releaseId,String configType,String contentRef){
    Integer count=jdbc.queryForObject("SELECT COUNT(*) FROM ops_config_release WHERE id=? AND config_type=? AND content_ref=? AND status='PUBLISHED'",Integer.class,releaseId,configType,contentRef);
    return count!=null&&count==1;
  }

  @Override
  public ManagedResource changeRuntimeStatus(String type,long id,long releaseId,String target,long expected,LocalDateTime now){
    requireUpdated(jdbc.update("UPDATE "+table(type)+" SET status=?,current_release_id=?,version=version+1,updated_at=? WHERE id=? AND version=? AND deleted=0",target,releaseId,now,id,expected));
    return new ManagedResource(id,type,String.valueOf(id),target,expected+1,now);
  }

  @Override
  public ManagedResource retire(String type,long id,long expected,LocalDateTime now){
    requireUpdated(jdbc.update("UPDATE "+table(type)+" SET status='RETIRED',deleted=1,deleted_at=?,version=version+1,updated_at=? WHERE id=? AND version=? AND deleted=0",now,now,id,expected));
    return new ManagedResource(id,type,String.valueOf(id),"RETIRED",expected+1,now);
  }

  @Override
  public TicketMessageResult replyTicket(long id,long historyId,long adminId,TicketReplyCommand c,LocalDateTime now){
    Integer exists=jdbc.queryForObject("SELECT COUNT(*) FROM ops_support_ticket WHERE id=? AND deleted=0",Integer.class,c.ticketId());
    if(exists==null||exists==0)throw new BusinessException("OPS_TICKET_NOT_FOUND","客服工单不存在");
    jdbc.update("INSERT INTO ops_support_ticket_message(id,ticket_id,sender_type,sender_id,content,internal_note,deleted,created_at) VALUES(?,?,'ADMIN',?,?,?,0,?)",id,c.ticketId(),adminId,c.content(),c.internalNote(),now);
    jdbc.update("INSERT INTO ops_support_ticket_history(id,ticket_id,action,operator_type,operator_id,detail,created_at) VALUES(?,?,'REPLY','ADMIN',?,?,?)",historyId,c.ticketId(),adminId,c.internalNote()?"INTERNAL_NOTE":"USER_REPLY",now);
    jdbc.update("UPDATE ops_support_ticket SET follow_up_at=?,updated_at=? WHERE id=? AND deleted=0",c.followUpAt(),now,c.ticketId());
    return new TicketMessageResult(id,c.ticketId(),c.internalNote(),now);
  }

  @Override
  public MetricResult recordMetric(long id,MetricCommand c,LocalDateTime now){
    jdbc.update("INSERT INTO ops_metric_daily(id,metric_date,metric_key,dimension_type,dimension_value,metric_value,sample_count,created_at,updated_at) VALUES(?,?,?,?,?,?,?,?,?) ON DUPLICATE KEY UPDATE metric_value=VALUES(metric_value),sample_count=VALUES(sample_count),updated_at=VALUES(updated_at)",
        id,c.metricDate(),c.metricKey(),c.dimensionType(),c.dimensionValue(),c.metricValue(),c.sampleCount(),now,now);
    return new MetricResult(c.metricDate(),c.metricKey(),c.dimensionType(),c.dimensionValue(),c.metricValue(),c.sampleCount());
  }

  private void requireUpdated(int count){if(count!=1)throw new BusinessException("OPS_RESOURCE_CONFLICT","治理资源已变化，请刷新后重试");}
  private String table(String type){String table=RESOURCE_TABLES.get(type);if(table==null)throw new BusinessException("OPS_RESOURCE_TYPE_INVALID","治理资源类型不合法");return table;}
  private Long nullableLong(java.sql.ResultSet rs,String column)throws java.sql.SQLException{long value=rs.getLong(column);return rs.wasNull()?null:value;}
}
