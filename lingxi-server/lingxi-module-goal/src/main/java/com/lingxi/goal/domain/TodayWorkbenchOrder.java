package com.lingxi.goal.domain;

import com.lingxi.goal.api.TodayItem;
import java.util.Comparator;

/**
 * 今日工作台的排序规则，集中在一处以便与产品口径对照。
 *
 * <p>PRD 要求「默认按需要先做、预计时长、用户时间段排序」，这三个诉求并不在同一层，
 * 因此按下述优先级串起来：
 *
 * <ol>
 *   <li><b>逾期优先于今天</b>：欠账先还，否则工作台会把昨天没做的悄悄藏起来；
 *   <li><b>需要先做</b>：被更多其他行动当作前置的行动先做，做它才能解锁后续；
 *   <li><b>用户时间段</b>：按用户自己设定的行动时刻升序；
 *   <li><b>预计时长升序</b>：同样条件下短的先做，降低启动成本；
 *   <li><b>实例标识</b>：稳定兜底，保证同一份数据每次顺序一致，前端不会莫名抖动。
 * </ol>
 *
 * <p>未填写预计时长的行动排在同层最后，而不是当成 0：把「没填」当成「很快」会误导用户。
 */
public final class TodayWorkbenchOrder {

  /** 工作台默认排序。 */
  public static final Comparator<TodayItem> COMPARATOR =
      Comparator.comparing(TodayItem::overdue, Comparator.reverseOrder())
          .thenComparing(TodayItem::unlocksOthers, Comparator.reverseOrder())
          .thenComparing(TodayItem::localTime)
          .thenComparing(
              item -> item.estimatedMinutes() == null ? Integer.MAX_VALUE : item.estimatedMinutes())
          .thenComparing(TodayItem::occurrenceId);

  private TodayWorkbenchOrder() {}
}
