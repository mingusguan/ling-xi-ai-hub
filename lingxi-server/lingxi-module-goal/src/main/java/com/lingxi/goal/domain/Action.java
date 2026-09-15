package com.lingxi.goal.domain;

import com.lingxi.goal.api.RecurrenceType;
import com.lingxi.kernel.BusinessException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Set;

/**
 * 行动定义聚合。
 *
 * <p>时间规则与定义细节分别封装为 {@link ActionSchedule} 与 {@link ActionDetail}；
 * 首周之后修改行动以「未来全部生效」的语义整体替换值对象，历史打卡事实不受影响。
 */
public record Action(
    long id,
    long goalId,
    long planVersionId,
    Long milestoneId,
    String clientKey,
    String title,
    ActionSchedule schedule,
    ActionDetail detail,
    ActionStatus status,
    long version,
    LocalDateTime createdAt,
    LocalDateTime updatedAt) {

  public Action {
    if (id <= 0
        || goalId <= 0
        || planVersionId <= 0
        || title == null
        || title.isBlank()
        || schedule == null) {
      throw new BusinessException("GOAL_INVALID_ACTION", "行动定义不合法");
    }
    detail = detail == null ? ActionDetail.defaults() : detail;
    // 首行动是「创建后立刻做完的一件小事」（PRD ONB-02），只能是一次性行动：
    // 允许它重复会让启动门槛变成又一条长期任务。这条约束必须落在聚合构造上，
    // 否则引导入口可以绕过 edited() 的校验直接建出重复首行动。
    if (detail.first() && schedule.recurrenceType() != RecurrenceType.ONCE) {
      throw new BusinessException("GOAL_INVALID_FIRST_ACTION", "首行动只能是一次性行动");
    }
  }

  // ---- 时间与重复规则：转发到 schedule，调用方无需关心值对象的拆分方式 ----

  public RecurrenceType recurrenceType() {
    return schedule.recurrenceType();
  }

  public Set<DayOfWeek> weekdays() {
    return schedule.weekdays();
  }

  public Integer intervalDays() {
    return schedule.intervalDays();
  }

  public LocalDate startDate() {
    return schedule.startDate();
  }

  public LocalDate endDate() {
    return schedule.endDate();
  }

  public LocalTime localTime() {
    return schedule.localTime();
  }

  public LocalTime endLocalTime() {
    return schedule.endLocalTime();
  }

  public String timezone() {
    return schedule.timezone();
  }

  /** 判断行动在给定本地日期是否发生。 */
  public boolean occursOn(LocalDate date) {
    return schedule.occursOn(date);
  }

  /** 行动是否仍在推进，即还能产生新的实例。 */
  public boolean schedulable() {
    return status == ActionStatus.ACTIVE;
  }

  /**
   * 编辑行动定义，「本次及未来」整体生效。
   *
   * <p>已取消的行动不允许再编辑；历史实例与打卡事实保留不变，只影响后续滚动生成。
   */
  public Action edited(String newTitle, ActionSchedule newSchedule, ActionDetail newDetail, LocalDateTime now) {
    if (status == ActionStatus.CANCELLED) {
      throw new BusinessException("GOAL_ACTION_NOT_EDITABLE", "已取消的行动不能编辑");
    }
    if (newTitle == null || newTitle.isBlank()) {
      throw new BusinessException("GOAL_INVALID_ACTION", "行动标题不能为空");
    }
    // 首行动是一次性的启动门槛（PRD ONB-02），不能通过编辑变成重复行动，
    // 否则「创建后立刻做完一件小事」会退化成又一条长期任务。
    if (detail.first() && newSchedule.recurrenceType() != RecurrenceType.ONCE) {
      throw new BusinessException("GOAL_INVALID_FIRST_ACTION", "首行动只能是一次性行动");
    }
    return new Action(
        id,
        goalId,
        planVersionId,
        milestoneId,
        clientKey,
        newTitle.trim(),
        newSchedule,
        newDetail == null ? detail : newDetail,
        status,
        version + 1,
        createdAt,
        now);
  }

  /** 只调整定义细节，时间规则保持不变。 */
  public Action withDetail(ActionDetail newDetail, LocalDateTime now) {
    return edited(title, schedule, newDetail, now);
  }

  /** 把行动整体移动到新的开始日期，重复规则与时刻不变。 */
  public Action movedTo(LocalDate newStartDate, LocalDateTime now) {
    if (newStartDate == null) {
      throw new BusinessException("GOAL_INVALID_ACTION_RANGE", "移动日期不能为空");
    }
    ActionSchedule moved =
        new ActionSchedule(
            schedule.recurrenceType(),
            schedule.weekdays(),
            schedule.intervalDays(),
            newStartDate,
            schedule.endDate(),
            schedule.localTime(),
            schedule.endLocalTime(),
            schedule.timezone());
    return edited(title, moved, detail, now);
  }

  /** 取消行动；已取消时重复调用是安全的空操作。 */
  public Action cancelled(LocalDateTime now) {
    if (status == ActionStatus.CANCELLED) {
      return this;
    }
    return new Action(
        id,
        goalId,
        planVersionId,
        milestoneId,
        clientKey,
        title,
        schedule,
        detail,
        ActionStatus.CANCELLED,
        version + 1,
        createdAt,
        now);
  }

  /** 复制出一个同计划内的新行动，仅客户端标识与标题需要调用方提供。 */
  public Action copyAs(long newId, String newClientKey, String newTitle, LocalDateTime now) {
    if (newId <= 0 || newClientKey == null || newClientKey.isBlank()) {
      throw new BusinessException("GOAL_INVALID_ACTION", "复制行动缺少标识");
    }
    return new Action(
        newId,
        goalId,
        planVersionId,
        milestoneId,
        newClientKey,
        newTitle == null || newTitle.isBlank() ? title : newTitle.trim(),
        schedule,
        detail,
        status,
        0,
        now,
        now);
  }
}
