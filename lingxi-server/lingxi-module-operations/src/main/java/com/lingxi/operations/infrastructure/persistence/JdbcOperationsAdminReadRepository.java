package com.lingxi.operations.infrastructure.persistence;

import com.lingxi.kernel.PageResult;
import com.lingxi.operations.api.OperationsAdminReadFacade.*;
import com.lingxi.operations.domain.OperationsAdminReadRepository;
import java.time.LocalDateTime;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** operations 内部后台读模型，不查询其他业务模块表。 */
@Repository
public class JdbcOperationsAdminReadRepository implements OperationsAdminReadRepository {
  private final JdbcTemplate jdbc;
  public JdbcOperationsAdminReadRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

  public DashboardSnapshot localDashboard() {
    long tickets = count("SELECT COUNT(*) FROM ops_support_ticket WHERE deleted=0 AND status NOT IN ('RESOLVED','CLOSED')");
    long releases = count("SELECT COUNT(*) FROM ops_config_release WHERE status IN ('DRAFT','VALIDATING','PENDING_APPROVAL','GRAY')");
    long risks = count("SELECT COUNT(*) FROM ops_safety_case WHERE deleted=0 AND risk_level IN ('HIGH','CRITICAL') AND status NOT IN ('RESOLVED','CLOSED')");
    long audits = count("SELECT COUNT(*) FROM ops_audit_log WHERE deleted=0 AND created_at>=UTC_DATE()");
    return new DashboardSnapshot(tickets, releases, risks, audits, 0, 0, 0, 0, 0, 0);
  }

  public PageResult<TicketSummary> tickets(String status, String category, int page, int size) {
    Query q = query(" WHERE deleted=0", status, "status", category, "category");
    long total = total("ops_support_ticket", q);
    q.args.add(size); q.args.add((page - 1) * size);
    var items = jdbc.query("SELECT id,ticket_no,user_id,category,subject,status,priority,assignee_admin_id,version,sla_due_at,follow_up_at,created_at,updated_at FROM ops_support_ticket" + q.sql + " ORDER BY updated_at DESC LIMIT ? OFFSET ?",
        (rs,row)->new TicketSummary(rs.getLong("id"),rs.getString("ticket_no"),rs.getLong("user_id"),
            rs.getString("category"),rs.getString("subject"),rs.getString("status"),rs.getString("priority"),
            nullableLong(rs,"assignee_admin_id"),rs.getLong("version"),time(rs,"sla_due_at"),time(rs,"follow_up_at"),time(rs,"created_at"),time(rs,"updated_at")),q.args.toArray());
    return new PageResult<>(items,total,page,size);
  }

  public PageResult<ReleaseSummary> releases(String type, String status, int page, int size) {
    Query q = query(" WHERE 1=1", type, "config_type", status, "status");
    long total = total("ops_config_release", q);
    q.args.add(size); q.args.add((page - 1) * size);
    var items = jdbc.query("SELECT * FROM ops_config_release" + q.sql + " ORDER BY updated_at DESC LIMIT ? OFFSET ?",
        (rs,row)->new ReleaseSummary(rs.getLong("id"),rs.getString("release_key"),rs.getString("config_type"),
            rs.getInt("version_no"),rs.getString("content_ref"),rs.getString("content_digest"),rs.getString("gray_rule"),
            rs.getString("status"),rs.getLong("created_by"),nullableLong(rs,"approved_by"),nullableLong(rs,"published_by"),
            nullableLong(rs,"previous_release_id"),rs.getString("failure_reason"),rs.getLong("lock_version"),
            time(rs,"created_at"),time(rs,"updated_at")),q.args.toArray());
    return new PageResult<>(items,total,page,size);
  }

  public PageResult<AuditSummary> audits(String action, Long operatorId, int page, int size) {
    List<Object> args = new ArrayList<>(); StringBuilder where = new StringBuilder(" WHERE deleted=0");
    if (action != null && !action.isBlank()) { where.append(" AND action=?"); args.add(action); }
    if (operatorId != null) { where.append(" AND admin_id=?"); args.add(operatorId); }
    long total = value(jdbc.queryForObject("SELECT COUNT(*) FROM ops_audit_log"+where,Long.class,args.toArray()));
    args.add(size);args.add((page-1)*size);
    var items=jdbc.query("SELECT id,admin_id,action,object_type,object_id,reason,ticket_no,result,request_id,created_at FROM ops_audit_log"+where+" ORDER BY created_at DESC LIMIT ? OFFSET ?",
        (rs,row)->new AuditSummary(rs.getLong("id"),rs.getLong("admin_id"),rs.getString("action"),rs.getString("object_type"),
            rs.getString("object_id"),rs.getString("reason"),rs.getString("ticket_no"),rs.getString("result"),
            rs.getString("request_id"),time(rs,"created_at")),args.toArray());
    return new PageResult<>(items,total,page,size);
  }

  public PageResult<SafetyCaseSummary> safetyCases(String status,String riskLevel,int page,int size){
    Query q=query(" WHERE deleted=0",status,"status",riskLevel,"risk_level");long total=total("ops_safety_case",q);
    q.args.add(size);q.args.add((page-1)*size);
    var items=jdbc.query("SELECT * FROM ops_safety_case"+q.sql+" ORDER BY FIELD(risk_level,'CRITICAL','HIGH','MEDIUM','LOW'),created_at ASC LIMIT ? OFFSET ?",
        (rs,row)->new SafetyCaseSummary(rs.getLong("id"),rs.getString("case_no"),nullableLong(rs,"user_id"),rs.getString("source_type"),
            rs.getString("risk_category"),rs.getString("risk_level"),rs.getString("content_excerpt"),rs.getString("status"),
            nullableLong(rs,"reviewer_admin_id"),rs.getString("resolution"),rs.getLong("version"),time(rs,"created_at"),time(rs,"updated_at")),q.args.toArray());
    return new PageResult<>(items,total,page,size);
  }

  public PageResult<ResourceSummary> resources(String type,String status,int page,int size){
    String table; String key; String name; String detail;
    switch(type==null?"":type){
      case "EXPERIMENT" -> {table="ops_experiment";key="experiment_key";name="experiment_key";detail="metrics_json";}
      case "CAMPAIGN" -> {table="ops_message_campaign";key="campaign_key";name="name";detail="audience_rule";}
      case "APP_RELEASE" -> {table="ops_app_release";key="CONCAT(platform,':',version_code)";name="version_name";detail="gray_rule";}
      case "COMPLIANCE" -> {table="ops_compliance_document";key="CONCAT(document_type,':',version_no)";name="title";detail="content_ref";}
      case "FEATURE_FLAG" -> {table="ops_feature_flag";key="flag_key";name="flag_key";detail="CAST(mandatory_policy AS CHAR)";}
      default -> throw new IllegalArgumentException("unsupported resource type");
    }
    List<Object> args=new ArrayList<>();String where=" WHERE deleted=0";
    if(status!=null&&!status.isBlank()){where+=" AND status=?";args.add(status);}
    long total=value(jdbc.queryForObject("SELECT COUNT(*) FROM "+table+where,Long.class,args.toArray()));
    args.add(size);args.add((page-1)*size);
    String version="version";
    String created="ops_feature_flag".equals(table)?"updated_at":"created_at";
    var items=jdbc.query("SELECT id,"+key+" resource_key,"+name+" resource_name,status,"+detail+" detail_json,"+version+" resource_version,"+created+" resource_created,updated_at FROM "+table+where+" ORDER BY updated_at DESC LIMIT ? OFFSET ?",
        (rs,row)->new ResourceSummary(rs.getLong("id"),type,rs.getString("resource_key"),rs.getString("resource_name"),
            rs.getString("status"),rs.getString("detail_json"),rs.getLong("resource_version"),time(rs,"resource_created"),time(rs,"updated_at")),args.toArray());
    return new PageResult<>(items,total,page,size);
  }

  public TicketDetail ticketDetail(long id){
    TicketSummary ticket=jdbc.query("SELECT id,ticket_no,user_id,category,subject,status,priority,assignee_admin_id,version,sla_due_at,follow_up_at,created_at,updated_at FROM ops_support_ticket WHERE id=? AND deleted=0",(rs,row)->new TicketSummary(rs.getLong("id"),rs.getString("ticket_no"),rs.getLong("user_id"),rs.getString("category"),rs.getString("subject"),rs.getString("status"),rs.getString("priority"),nullableLong(rs,"assignee_admin_id"),rs.getLong("version"),time(rs,"sla_due_at"),time(rs,"follow_up_at"),time(rs,"created_at"),time(rs,"updated_at")),id).stream().findFirst().orElseThrow(()->new com.lingxi.kernel.BusinessException("OPS_TICKET_NOT_FOUND","客服工单不存在"));
    var messages=jdbc.query("SELECT id,sender_type,sender_id,content,internal_note,created_at FROM ops_support_ticket_message WHERE ticket_id=? AND deleted=0 ORDER BY created_at",(rs,row)->new TicketMessageSummary(rs.getLong("id"),rs.getString("sender_type"),rs.getLong("sender_id"),rs.getString("content"),rs.getBoolean("internal_note"),time(rs,"created_at")),id);
    var history=jdbc.query("SELECT id,action,from_status,to_status,operator_type,operator_id,detail,created_at FROM ops_support_ticket_history WHERE ticket_id=? ORDER BY created_at",(rs,row)->new TicketHistorySummary(rs.getLong("id"),rs.getString("action"),rs.getString("from_status"),rs.getString("to_status"),rs.getString("operator_type"),rs.getLong("operator_id"),rs.getString("detail"),time(rs,"created_at")),id);
    return new TicketDetail(ticket,messages,history);
  }

  public List<MetricSeries> metrics(String key,String dimension,LocalDateTime from,LocalDateTime to){List<Object>a=new ArrayList<>();String w=" WHERE 1=1";if(key!=null&&!key.isBlank()){w+=" AND metric_key=?";a.add(key);}if(dimension!=null&&!dimension.isBlank()){w+=" AND dimension_type=?";a.add(dimension);}if(from!=null){w+=" AND metric_date>=?";a.add(from.toLocalDate());}if(to!=null){w+=" AND metric_date<=?";a.add(to.toLocalDate());}a.add(10000);return jdbc.query("SELECT metric_date,metric_key,dimension_type,dimension_value,metric_value,sample_count FROM ops_metric_daily"+w+" ORDER BY metric_date,metric_key,dimension_value LIMIT ?",(rs,row)->new MetricSeries(rs.getObject("metric_date",java.time.LocalDate.class),rs.getString("metric_key"),rs.getString("dimension_type"),rs.getString("dimension_value"),rs.getBigDecimal("metric_value"),rs.getLong("sample_count")),a.toArray());}

  public PageResult<SafetyAlertSummary> safetyAlerts(String status,int page,int size){List<Object>a=new ArrayList<>();String w=" WHERE 1=1";if(status!=null&&!status.isBlank()){w+=" AND status=?";a.add(status);}Long total=jdbc.queryForObject("SELECT COUNT(*) FROM ops_safety_alert"+w,Long.class,a.toArray());a.add(size);a.add((page-1)*size);var items=jdbc.query("SELECT * FROM ops_safety_alert"+w+" ORDER BY created_at DESC LIMIT ? OFFSET ?",(rs,row)->new SafetyAlertSummary(rs.getLong("id"),rs.getLong("safety_case_id"),rs.getString("alert_channel"),rs.getString("recipient_ref"),rs.getString("status"),rs.getInt("attempt_count"),rs.getString("failure_reason"),rs.getLong("version"),time(rs,"created_at"),time(rs,"updated_at")),a.toArray());return new PageResult<>(items,total==null?0:total,page,size);}

  private Query query(String base,String first,String firstColumn,String second,String secondColumn){
    Query q=new Query(base);if(first!=null&&!first.isBlank()){q.sql+=" AND "+firstColumn+"=?";q.args.add(first);}
    if(second!=null&&!second.isBlank()){q.sql+=" AND "+secondColumn+"=?";q.args.add(second);}return q;
  }
  private long total(String table,Query q){return value(jdbc.queryForObject("SELECT COUNT(*) FROM "+table+q.sql,Long.class,q.args.toArray()));}
  private long count(String sql){return value(jdbc.queryForObject(sql,Long.class));}
  private long value(Long value){return value==null?0:value;}
  private LocalDateTime time(java.sql.ResultSet rs,String column)throws java.sql.SQLException{return rs.getObject(column,LocalDateTime.class);}
  private Long nullableLong(java.sql.ResultSet rs,String column)throws java.sql.SQLException{long v=rs.getLong(column);return rs.wasNull()?null:v;}
  private static final class Query{private String sql;private final List<Object> args=new ArrayList<>();private Query(String sql){this.sql=sql;}}
}
