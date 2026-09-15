package com.lingxi.goal.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import org.junit.jupiter.api.Test;

/**
 * 计划行动草案的请求体反序列化验证。
 *
 * <p>回归背景：间隔重复上线后，端到端联调出现「请求体带 intervalDays=3、服务端仍报
 * GOAL_INTERVAL_REQUIRED」的假象。该用例把 HTTP 请求体的真实形状固定下来，
 * 一旦记录组件改名、增删或顺序变化导致字段无法绑定，这里会先失败。
 */
class PlanActionDraftJsonTest {
  private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());

  @Test
  void bindsEveryComponentFromRequestJson() throws Exception {
    String json =
        "{\"clientKey\":\"a-interval\",\"milestoneSequence\":null,\"title\":\"Every third day\","
            + "\"recurrenceType\":\"INTERVAL\",\"weekdays\":[],\"intervalDays\":3,"
            + "\"startDate\":\"2026-09-14\",\"endDate\":null,\"localTime\":\"07:00:00\","
            + "\"timezone\":\"Asia/Shanghai\"}";

    PlanActionDraft draft = mapper.readValue(json, PlanActionDraft.class);

    assertThat(draft.clientKey()).isEqualTo("a-interval");
    assertThat(draft.milestoneSequence()).isNull();
    assertThat(draft.title()).isEqualTo("Every third day");
    assertThat(draft.recurrenceType()).isEqualTo(RecurrenceType.INTERVAL);
    assertThat(draft.weekdays()).isEmpty();
    assertThat(draft.intervalDays()).isEqualTo(3);
    assertThat(draft.startDate()).isEqualTo(LocalDate.of(2026, 9, 14));
    assertThat(draft.endDate()).isNull();
    assertThat(draft.localTime()).isEqualTo(LocalTime.of(7, 0));
    assertThat(draft.timezone()).isEqualTo("Asia/Shanghai");
    // 请求体没带 first 时必须按「普通行动」处理；缺省值退化成 true 会把 5—30 分钟约束
    // 意外加到所有行动上，因此这里把缺省语义固定住。
    assertThat(draft.first()).isFalse();
  }

  /** 首行动标记必须能从请求体绑定，否则 ONB-02 的 5—30 分钟约束在 HTTP 层失效。 */
  @Test
  void bindsFirstActionFlagFromRequestJson() throws Exception {
    String json =
        "{\"clientKey\":\"first-action\",\"milestoneSequence\":null,\"title\":\"Read ten minutes\","
            + "\"recurrenceType\":\"ONCE\",\"weekdays\":[],\"intervalDays\":null,"
            + "\"startDate\":\"2026-09-15\",\"endDate\":null,\"localTime\":\"20:00:00\","
            + "\"timezone\":\"Asia/Shanghai\",\"estimatedMinutes\":10,\"first\":true}";

    PlanActionDraft draft = mapper.readValue(json, PlanActionDraft.class);

    assertThat(draft.first()).isTrue();
    assertThat(draft.estimatedMinutes()).isEqualTo(10);
    assertThat(draft.recurrenceType()).isEqualTo(RecurrenceType.ONCE);
  }

  /** 每周重复必须绑定星期集合，否则领域层会拒绝该行动。 */
  @Test
  void bindsWeeklyWeekdaysFromRequestJson() throws Exception {
    String json =
        "{\"clientKey\":\"a-weekly\",\"milestoneSequence\":null,\"title\":\"Weekly review\","
            + "\"recurrenceType\":\"WEEKLY\",\"weekdays\":[\"MONDAY\",\"THURSDAY\"],"
            + "\"intervalDays\":null,\"startDate\":\"2026-09-14\",\"endDate\":null,"
            + "\"localTime\":\"20:00:00\",\"timezone\":\"Asia/Shanghai\"}";

    PlanActionDraft draft = mapper.readValue(json, PlanActionDraft.class);

    assertThat(draft.weekdays()).containsExactlyInAnyOrder(DayOfWeek.MONDAY, DayOfWeek.THURSDAY);
    assertThat(draft.intervalDays()).isNull();
  }
}
