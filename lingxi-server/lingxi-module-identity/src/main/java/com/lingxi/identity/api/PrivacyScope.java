package com.lingxi.identity.api;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/** 隐私批量请求的模块范围；空集合表示所有业务模块。 */
public record PrivacyScope(Set<String> modules) {
  public PrivacyScope {
    modules = Collections.unmodifiableSet(new LinkedHashSet<>(modules == null ? Set.of() : modules));
  }

  public boolean includes(String moduleName) {
    return modules.isEmpty() || modules.contains(moduleName);
  }

  public boolean isAllModules() {
    return modules.isEmpty();
  }
}
