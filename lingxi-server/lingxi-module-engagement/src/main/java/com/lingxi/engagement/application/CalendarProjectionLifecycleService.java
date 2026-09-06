package com.lingxi.engagement.application;

import com.lingxi.engagement.api.CalendarProjectionCommand;
import com.lingxi.engagement.domain.*;
import com.lingxi.kernel.*;
import java.time.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

@Service
public class CalendarProjectionLifecycleService {
  private final EngagementRepository repo;
  private final IdGenerator ids;

  public CalendarProjectionLifecycleService(EngagementRepository r, IdGenerator i) {
    repo = r;
    ids = i;
  }

  @Transactional(readOnly = true)
  public Context load(CalendarProjectionCommand c) {
    CalendarBinding b =
        repo.findCalendar(c.bindingId())
            .orElseThrow(() -> new BusinessException("ENG_CALENDAR_NOT_FOUND", "日历绑定不存在"));
    boolean cleanup =
        "DELETE".equals(c.operation())
            && b.getStatus() == CalendarBinding.Status.REVOKED
            && b.isDeleteCreatedEvents();
    if (b.getUserId() != c.userId() || b.getStatus() != CalendarBinding.Status.ACTIVE && !cleanup)
      throw new BusinessException("ENG_CALENDAR_REVOKED", "日历绑定不可写");
    return new Context(
        b, repo.findCalendarEvent(c.bindingId(), c.resourceType(), c.resourceId()).orElse(null));
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void complete(
      CalendarProjectionCommand c,
      CalendarProviderAdapter.ProjectionReceipt receipt,
      boolean deleted) {
    CalendarEventProjection old =
        repo.findCalendarEvent(c.bindingId(), c.resourceType(), c.resourceId()).orElse(null);
    if (deleted) {
      if (old != null) repo.deleteCalendarEvent(old.id());
      return;
    }
    long id = old == null ? ids.nextId() : old.id();
    repo.upsertCalendarEvent(
        new CalendarEventProjection(
            id,
            c.bindingId(),
            c.resourceType(),
            c.resourceId(),
            receipt.externalId(),
            receipt.externalVersion(),
            "SYNCED",
            LocalDateTime.now(ZoneOffset.UTC)));
  }

  public record Context(CalendarBinding binding, CalendarEventProjection event) {}
}
