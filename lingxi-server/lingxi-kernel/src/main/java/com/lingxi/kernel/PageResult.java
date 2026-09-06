package com.lingxi.kernel;

import java.util.List;

/** 跨模块统一分页结果，页码从 1 开始。 */
public record PageResult<T>(List<T> items, long total, int page, int pageSize) {
  public PageResult {
    items = items == null ? List.of() : List.copyOf(items);
    if (total < 0 || page < 1 || pageSize < 1 || pageSize > 200) {
      throw new IllegalArgumentException("invalid pagination");
    }
  }
}
