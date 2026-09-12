package com.lingxi.operations.domain;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface OperationsRepository {
  Optional<SupportTicket> findTicket(long id);

  /** 统计指定用户的工单总数，用于“我的工单”列表分页。 */
  long countTicketsByUser(long userId);

  /** 按创建时间倒序分页查询指定用户的工单。 */
  List<SupportTicket> findTicketsByUser(long userId, int page, int pageSize);

  void insertTicket(SupportTicket ticket);

  boolean updateTicket(SupportTicket ticket, long previous);

  void appendTicketHistory(long id,long ticketId,String action,String fromStatus,
      String toStatus,String operatorType,long operatorId,String detail,LocalDateTime now);

  void appendTicketMessage(long id,long ticketId,String senderType,long senderId,
      String content,boolean internalNote,LocalDateTime now);

  Optional<Long> findTicketPrivacyRequestId(long ticketId);

  Optional<ConfigRelease> findRelease(long id);

  Optional<ConfigRelease> findReleaseByKey(String key);

  Optional<ConfigRelease> findPublished(String type);

  void insertRelease(ConfigRelease release);

  boolean updateRelease(ConfigRelease release, long previous);

  void appendAudit(
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
      LocalDateTime now);
}
