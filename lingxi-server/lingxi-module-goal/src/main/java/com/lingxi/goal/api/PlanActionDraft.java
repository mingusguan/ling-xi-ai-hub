package com.lingxi.goal.api;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Set;

/**
 * 待确认计划中的行动草案。
 *
 * <p>前置行动用 {@code prerequisiteClientKey} 而不是标识引用：客户端在同一份草案里
 * 还不知道服务端将要分配的标识，只能按 clientKey 指向另一个草案，
 * 由应用层在写入后统一解析成真实标识。
 */
public record PlanActionDraft(
    String clientKey,
    Integer milestoneSequence,
    String title,
    String description,
    RecurrenceType recurrenceType,
    Set<DayOfWeek> weekdays,
    /** 间隔重复的间隔天数，仅 recurrenceType=INTERVAL 时必填。 */
    Integer intervalDays,
    LocalDate startDate,
    LocalDate endDate,
    LocalTime localTime,
    /** 时间段结束时刻；为空表示只安排开始时刻。 */
    LocalTime endLocalTime,
    String timezone,
    PriorityLevel priority,
    ActionDifficulty difficulty,
    String completionCriteria,
    Integer estimatedMinutes,
    /** 前置行动的客户端标识，必须指向同一份草案中的另一个行动。 */
    String prerequisiteClientKey,
    String reminderPolicy,
    /**
     * 是否为新手引导的首行动（PRD ONB-02）。
     *
     * <p>为 true 时服务端强制三条约束：必须是一次性行动、必须给出预计时长、
     * 且预计时长落在 5—30 分钟内。缺省 false，普通行动不受影响。
     */
    boolean first) {}
