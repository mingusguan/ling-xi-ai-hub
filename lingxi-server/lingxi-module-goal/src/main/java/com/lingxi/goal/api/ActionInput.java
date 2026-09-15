package com.lingxi.goal.api;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Set;

/**
 * 行动定义入参，新增与编辑共用。
 *
 * <p>「本次及未来」的编辑整体替换这份定义；单次调整不经过这里，而是走例外接口，
 * 保证一次临时改期不会污染整个重复规则。
 */
public record ActionInput(
    String title,
    String description,
    RecurrenceType recurrenceType,
    Set<DayOfWeek> weekdays,
    Integer intervalDays,
    LocalDate startDate,
    LocalDate endDate,
    LocalTime localTime,
    LocalTime endLocalTime,
    String timezone,
    PriorityLevel priority,
    ActionDifficulty difficulty,
    String completionCriteria,
    Integer estimatedMinutes,
    /** 前置行动标识；编辑时可给出同一目标下已有行动的标识。 */
    Long prerequisiteActionId,
    String reminderPolicy,
    /**
     * 是否为新手引导的首行动（PRD ONB-02）。
     *
     * <p>为 true 时服务端强制三条约束：必须是一次性行动、必须给出预计时长、
     * 且预计时长落在 5—30 分钟内。缺省 false，普通行动不受影响。
     */
    boolean first) {}
