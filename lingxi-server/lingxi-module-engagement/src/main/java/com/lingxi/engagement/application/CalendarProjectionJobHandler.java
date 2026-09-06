package com.lingxi.engagement.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lingxi.engagement.api.CalendarProjectionCommand;
import com.lingxi.kernel.*;
import java.util.*;
import org.springframework.stereotype.Component;

@Component
public class CalendarProjectionJobHandler implements AsyncJobHandler {
  public static final String JOB_TYPE = "engagement.calendar.projection";
  private final CalendarProjectionLifecycleService lifecycle;
  private final Map<String, CalendarProviderAdapter> adapters = new HashMap<>();
  private final ObjectMapper json;

  public CalendarProjectionJobHandler(
      CalendarProjectionLifecycleService l, List<CalendarProviderAdapter> a, ObjectMapper j) {
    lifecycle = l;
    a.forEach(x -> adapters.put(x.provider(), x));
    json = j;
  }

  public boolean supports(String type) {
    return JOB_TYPE.equals(type);
  }

  public String handle(AsyncJobMessage m) throws Exception {
    CalendarProjectionCommand c = json.readValue(m.payloadJson(), CalendarProjectionCommand.class);
    var context = lifecycle.load(c);
    CalendarProviderAdapter adapter = adapters.get(context.binding().getProvider());
    if (adapter == null)
      throw new BusinessException("ENG_CALENDAR_PROVIDER_UNAVAILABLE", "日历渠道尚未配置");
    if ("DELETE".equals(c.operation())) {
      if (context.event() != null)
        adapter.delete(
            context.binding().getCredentialReference(),
            context.event().externalId(),
            c.requestKey());
      lifecycle.complete(c, null, true);
    } else {
      var receipt =
          adapter.upsert(
              context.binding().getCredentialReference(),
              c,
              context.event() == null ? null : context.event().externalId(),
              context.event() == null ? null : context.event().externalVersion());
      lifecycle.complete(c, receipt, false);
    }
    return "{\"status\":\"synced\"}";
  }
}
