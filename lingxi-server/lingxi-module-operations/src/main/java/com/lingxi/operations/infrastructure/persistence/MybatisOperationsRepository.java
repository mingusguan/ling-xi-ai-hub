package com.lingxi.operations.infrastructure.persistence;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lingxi.operations.domain.*;
import java.time.*;
import java.util.*;
import org.springframework.stereotype.Repository;
import org.springframework.jdbc.core.JdbcTemplate;

@Repository
public class MybatisOperationsRepository implements OperationsRepository {
  private final SupportTicketMapper tickets;
  private final ConfigReleaseMapper releases;
  private final AuditLogMapper audits;
  private final JdbcTemplate jdbc;

  public MybatisOperationsRepository(
      SupportTicketMapper t, ConfigReleaseMapper r, AuditLogMapper a, JdbcTemplate jdbc) {
    tickets = t;
    releases = r;
    audits = a;
    this.jdbc = jdbc;
  }

  public Optional<SupportTicket> findTicket(long id) {
    return Optional.ofNullable(tickets.selectById(id)).map(this::ticket);
  }

  public void insertTicket(SupportTicket t) {
    SupportTicketEntity e = new SupportTicketEntity();
    fill(e, t);
    tickets.insert(e);
  }

  public boolean updateTicket(SupportTicket t, long v) {
    return tickets.update(
            null,
            Wrappers.<SupportTicketEntity>lambdaUpdate()
                .eq(SupportTicketEntity::getId, t.getId())
                .eq(SupportTicketEntity::getVersion, v)
                .set(SupportTicketEntity::getStatus, t.getStatus().name())
                .set(SupportTicketEntity::getAssigneeAdminId, t.getAssigneeAdminId())
                .set(SupportTicketEntity::getVersion, t.getVersion())
                .set(SupportTicketEntity::getUpdatedAt, t.getUpdatedAt()))
        == 1;
  }

  @Override
  public void appendTicketMessage(long id,long ticketId,String senderType,long senderId,
      String content,boolean internalNote,LocalDateTime now){
    jdbc.update("INSERT INTO ops_support_ticket_message(id,ticket_id,sender_type,sender_id,content,internal_note,deleted,created_at) VALUES(?,?,?,?,?,?,0,?)",
        id,ticketId,senderType,senderId,content,internalNote?1:0,now);
  }

  @Override
  public void appendTicketHistory(long id,long ticketId,String action,String fromStatus,
      String toStatus,String operatorType,long operatorId,String detail,LocalDateTime now){
    jdbc.update("INSERT INTO ops_support_ticket_history(id,ticket_id,action,from_status,to_status,operator_type,operator_id,detail,created_at) VALUES(?,?,?,?,?,?,?,?,?)",
        id,ticketId,action,fromStatus,toStatus,operatorType,operatorId,detail,now);
  }

  @Override
  public Optional<Long> findTicketPrivacyRequestId(long ticketId) {
    SupportTicketEntity entity =
        tickets.selectOne(
            Wrappers.<SupportTicketEntity>lambdaQuery()
                .select(SupportTicketEntity::getPrivacyRequestId)
                .eq(SupportTicketEntity::getId, ticketId)
                .last("LIMIT 1"));
    return entity == null ? Optional.empty() : Optional.ofNullable(entity.getPrivacyRequestId());
  }

  public Optional<ConfigRelease> findRelease(long id) {
    return Optional.ofNullable(releases.selectById(id)).map(this::release);
  }

  public Optional<ConfigRelease> findReleaseByKey(String key) {
    return Optional.ofNullable(
            releases.selectOne(
                Wrappers.<ConfigReleaseEntity>lambdaQuery()
                    .eq(ConfigReleaseEntity::getReleaseKey, key)
                    .last("LIMIT 1")))
        .map(this::release);
  }

  public Optional<ConfigRelease> findPublished(String type) {
    return Optional.ofNullable(
            releases.selectOne(
                Wrappers.<ConfigReleaseEntity>lambdaQuery()
                    .eq(ConfigReleaseEntity::getConfigType, type)
                    .eq(ConfigReleaseEntity::getStatus, "PUBLISHED")
                    .orderByDesc(ConfigReleaseEntity::getVersionNo)
                    .last("LIMIT 1")))
        .map(this::release);
  }

  public void insertRelease(ConfigRelease r) {
    ConfigReleaseEntity e = new ConfigReleaseEntity();
    fill(e, r);
    releases.insert(e);
  }

  public boolean updateRelease(ConfigRelease r, long v) {
    return releases.update(
            null,
            Wrappers.<ConfigReleaseEntity>lambdaUpdate()
                .eq(ConfigReleaseEntity::getId, r.getId())
                .eq(ConfigReleaseEntity::getLockVersion, v)
                .set(ConfigReleaseEntity::getStatus, r.getStatus().name())
                .set(ConfigReleaseEntity::getApprovedBy, r.getApprovedBy())
                .set(ConfigReleaseEntity::getPublishedBy, r.getPublishedBy())
                .set(ConfigReleaseEntity::getPreviousReleaseId, r.getPreviousReleaseId())
                .set(ConfigReleaseEntity::getFailureReason, r.getFailureReason())
                .set(ConfigReleaseEntity::getLockVersion, r.getVersion())
                .set(ConfigReleaseEntity::getUpdatedAt, r.getUpdatedAt()))
        == 1;
  }

  public void appendAudit(
      long id,
      long admin,
      String action,
      String objectType,
      String objectId,
      String reason,
      String ticket,
      String before,
      String after,
      String result,
      String requestId,
      LocalDateTime now) {
    AuditLogEntity e = new AuditLogEntity();
    e.setId(id);
    e.setAdminId(admin);
    e.setAction(action);
    e.setObjectType(objectType);
    e.setObjectId(objectId);
    e.setReason(reason);
    e.setTicketNo(ticket);
    e.setBeforeDigest(before);
    e.setAfterDigest(after);
    e.setResult(result);
    e.setRequestId(requestId);
    e.setCreatedAt(now);
    audits.insert(e);
  }

  private SupportTicket ticket(SupportTicketEntity e) {
    return SupportTicket.rehydrate(
        e.getId(),
        e.getTicketNo(),
        e.getUserId(),
        e.getCategory(),
        e.getSubject(),
        e.getDescription(),
        e.getPriority(),
        SupportTicket.Status.valueOf(e.getStatus()),
        e.getAssigneeAdminId(),
        e.getSlaDueAt(),
        e.getVersion(),
        e.getCreatedAt(),
        e.getUpdatedAt());
  }

  private void fill(SupportTicketEntity e, SupportTicket t) {
    e.setId(t.getId());
    e.setTicketNo(t.getTicketNo());
    e.setUserId(t.getUserId());
    e.setCategory(t.getCategory());
    e.setSubject(t.getSubject());
    e.setDescription(t.getDescription());
    e.setPriority(t.getPriority());
    e.setStatus(t.getStatus().name());
    e.setAssigneeAdminId(t.getAssigneeAdminId());
    e.setVersion(t.getVersion());
    e.setSlaDueAt(t.getSlaDueAt());
    e.setCreatedAt(t.getCreatedAt());
    e.setUpdatedAt(t.getUpdatedAt());
  }

  private ConfigRelease release(ConfigReleaseEntity e) {
    return ConfigRelease.rehydrate(
        e.getId(),
        e.getReleaseKey(),
        e.getConfigType(),
        e.getVersionNo(),
        e.getContentRef(),
        e.getContentDigest(),
        e.getGrayRule(),
        ConfigRelease.Status.valueOf(e.getStatus()),
        e.getCreatedBy(),
        e.getApprovedBy(),
        e.getPublishedBy(),
        e.getPreviousReleaseId(),
        e.getFailureReason(),
        e.getLockVersion(),
        e.getCreatedAt(),
        e.getUpdatedAt());
  }

  private void fill(ConfigReleaseEntity e, ConfigRelease r) {
    e.setId(r.getId());
    e.setReleaseKey(r.getReleaseKey());
    e.setConfigType(r.getConfigType());
    e.setVersionNo(r.getVersionNo());
    e.setContentRef(r.getContentRef());
    e.setContentDigest(r.getContentDigest());
    e.setGrayRule(r.getGrayRule());
    e.setStatus(r.getStatus().name());
    e.setCreatedBy(r.getCreatedBy());
    e.setApprovedBy(r.getApprovedBy());
    e.setPublishedBy(r.getPublishedBy());
    e.setPreviousReleaseId(r.getPreviousReleaseId());
    e.setFailureReason(r.getFailureReason());
    e.setLockVersion(r.getVersion());
    e.setCreatedAt(r.getCreatedAt());
    e.setUpdatedAt(r.getUpdatedAt());
  }
}
