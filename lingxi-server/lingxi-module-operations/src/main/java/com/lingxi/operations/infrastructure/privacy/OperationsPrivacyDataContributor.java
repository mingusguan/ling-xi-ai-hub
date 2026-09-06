package com.lingxi.operations.infrastructure.privacy;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lingxi.identity.api.*;
import com.lingxi.kernel.IdGenerator;
import com.lingxi.operations.infrastructure.persistence.*;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** 处理用户工单和以用户身份产生的操作审计，平台配置发布记录不在用户隐私范围内。 */
@Component
public class OperationsPrivacyDataContributor
    implements PrivacyDataContributor, PrivacyManualReviewPort {
  private final SupportTicketMapper ticketMapper;
  private final ObjectMapper objectMapper;
  private final IdGenerator idGenerator;

  public OperationsPrivacyDataContributor(
      SupportTicketMapper ticketMapper, ObjectMapper objectMapper, IdGenerator idGenerator) {
    this.ticketMapper = ticketMapper;
    this.objectMapper = objectMapper;
    this.idGenerator = idGenerator;
  }

  @Override public String moduleName() { return "operations"; }

  @Override
  @Transactional
  public PrivacyContribution process(PrivacyProcessingContext context) {
    long userId = context.userId();
    PrivacyRequestType type = context.type();
    if (type == PrivacyRequestType.CORRECTION) {
      return PrivacyContribution.pending(ensureCorrectionTicket(context));
    }
    if (type == PrivacyRequestType.EXPORT) {
      Map<String, Object> data = new LinkedHashMap<>();
      data.put("supportTickets", ticketMapper.selectList(
          Wrappers.<SupportTicketEntity>lambdaQuery().eq(SupportTicketEntity::getUserId, userId)));
      try {
        return PrivacyContribution.exported(objectMapper.writeValueAsString(data));
      } catch (JsonProcessingException e) {
        throw new IllegalStateException("运营数据导出失败", e);
      }
    }
    if (type != PrivacyRequestType.CLOSE_ACCOUNT) {
      return PrivacyContribution.unchanged();
    }
    int affectedRows = ticketMapper.delete(
        Wrappers.<SupportTicketEntity>lambdaQuery().eq(SupportTicketEntity::getUserId, userId));
    return PrivacyContribution.deleted(affectedRows);
  }

  private String ensureCorrectionTicket(PrivacyProcessingContext context) {
    String ticketNo = "PRIV-CORR-" + context.requestId();
    SupportTicketEntity existing =
        ticketMapper.selectOne(
            Wrappers.<SupportTicketEntity>lambdaQuery()
                .eq(SupportTicketEntity::getTicketNo, ticketNo)
                .last("LIMIT 1"));
    if (existing != null) {
      return "support-ticket:" + existing.getId();
    }
    LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
    SupportTicketEntity ticket = new SupportTicketEntity();
    ticket.setId(idGenerator.nextId());
    ticket.setTicketNo(ticketNo);
    ticket.setUserId(context.userId());
    ticket.setPrivacyRequestId(context.requestId());
    ticket.setCategory("PRIVACY_CORRECTION");
    ticket.setSubject("用户数据更正请求");
    ticket.setDescription("请求模块：" + context.scope().modules());
    ticket.setStatus("OPEN");
    ticket.setPriority("HIGH");
    ticket.setVersion(0L);
    ticket.setCreatedAt(now);
    ticket.setUpdatedAt(now);
    ticketMapper.insert(ticket);
    return "support-ticket:" + ticket.getId();
  }

  @Override
  @Transactional
  public String open(long requestId, long userId, String category, String description) {
    String ticketNo = "PRIV-" + requestId + "-" + category;
    SupportTicketEntity existing =
        ticketMapper.selectOne(
            Wrappers.<SupportTicketEntity>lambdaQuery()
                .eq(SupportTicketEntity::getTicketNo, ticketNo)
                .last("LIMIT 1"));
    if (existing != null) {
      return "support-ticket:" + existing.getId();
    }
    LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
    SupportTicketEntity ticket = new SupportTicketEntity();
    ticket.setId(idGenerator.nextId());
    ticket.setTicketNo(ticketNo);
    ticket.setUserId(userId);
    ticket.setPrivacyRequestId(requestId);
    ticket.setCategory(category);
    ticket.setSubject("隐私请求人工复核");
    ticket.setDescription(description);
    ticket.setStatus("OPEN");
    ticket.setPriority("HIGH");
    ticket.setVersion(0L);
    ticket.setCreatedAt(now);
    ticket.setUpdatedAt(now);
    ticketMapper.insert(ticket);
    return "support-ticket:" + ticket.getId();
  }
}
