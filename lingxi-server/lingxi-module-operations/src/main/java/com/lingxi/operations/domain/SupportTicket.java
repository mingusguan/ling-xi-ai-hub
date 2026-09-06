package com.lingxi.operations.domain;

import com.lingxi.kernel.BusinessException;
import java.time.*;

public class SupportTicket {
  public enum Status {
    OPEN,
    IN_PROGRESS,
    WAITING_USER,
    RESOLVED,
    CLOSED
  }

  private final long id, userId;
  private final String ticketNo, category, subject, description, priority;
  private Status status;
  private Long assigneeAdminId;
  private final LocalDateTime slaDueAt;
  private long version;
  private final LocalDateTime createdAt;
  private LocalDateTime updatedAt;

  private SupportTicket(
      long id,
      String no,
      long user,
      String category,
      String subject,
      String description,
      String priority,
      Status status,
      Long assignee,
      LocalDateTime slaDueAt,
      long version,
      LocalDateTime created,
      LocalDateTime updated) {
    this.id = id;
    ticketNo = no;
    userId = user;
    this.category = category;
    this.subject = subject;
    this.description = description;
    this.priority = priority;
    this.status = status;
    assigneeAdminId = assignee;
    this.slaDueAt = slaDueAt;
    this.version = version;
    createdAt = created;
    updatedAt = updated;
  }

  public static SupportTicket create(
      long id,
      String no,
      long user,
      String category,
      String subject,
      String description,
      String priority,
      LocalDateTime now) {
    if (id <= 0
        || user <= 0
        || subject == null
        || subject.isBlank()
        || description == null
        || description.isBlank()) throw new BusinessException("OPS_INVALID_TICKET", "工单参数不合法");
    LocalDateTime slaDueAt = now.plus(slaDuration(priority));
    return new SupportTicket(
        id, no, user, category, subject, description, priority, Status.OPEN, null, slaDueAt, 0, now, now);
  }

  public static SupportTicket rehydrate(
      long id,
      String no,
      long user,
      String category,
      String subject,
      String description,
      String priority,
      Status status,
      Long assignee,
      LocalDateTime slaDueAt,
      long version,
      LocalDateTime created,
      LocalDateTime updated) {
    return new SupportTicket(
        id,
        no,
        user,
        category,
        subject,
        description,
        priority,
        status,
        assignee,
        slaDueAt,
        version,
        created,
        updated);
  }

  public void transition(Status target, Long assignee, long expected, LocalDateTime now) {
    if (version != expected) throw new BusinessException("OPS_TICKET_CONFLICT", "工单已变化");
    if (status == Status.CLOSED)
      throw new BusinessException("OPS_TICKET_STATE_INVALID", "已关闭工单不可修改");
    status = target;
    assigneeAdminId = assignee;
    version++;
    updatedAt = now;
  }

  public void owner(long user) {
    if (userId != user) throw new BusinessException("OPS_TICKET_NOT_FOUND", "工单不存在");
  }

  public long getId() {
    return id;
  }

  public String getTicketNo() {
    return ticketNo;
  }

  public long getUserId() {
    return userId;
  }

  public String getCategory() {
    return category;
  }

  public String getSubject() {
    return subject;
  }

  public String getDescription() {
    return description;
  }

  public String getPriority() {
    return priority;
  }

  public Status getStatus() {
    return status;
  }

  public Long getAssigneeAdminId() {
    return assigneeAdminId;
  }

  public long getVersion() {
    return version;
  }

  public LocalDateTime getSlaDueAt() { return slaDueAt; }

  private static Duration slaDuration(String priority) {
    return switch (priority == null ? "" : priority) {
      case "URGENT" -> Duration.ofHours(1);
      case "HIGH" -> Duration.ofHours(4);
      case "NORMAL" -> Duration.ofHours(24);
      case "LOW" -> Duration.ofHours(48);
      default -> throw new BusinessException("OPS_TICKET_PRIORITY_INVALID", "工单优先级不合法");
    };
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public LocalDateTime getUpdatedAt() {
    return updatedAt;
  }
}
