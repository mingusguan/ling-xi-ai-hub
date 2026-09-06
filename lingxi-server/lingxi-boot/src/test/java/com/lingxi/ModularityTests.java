package com.lingxi;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

/** 阻止跨模块内部包访问、未声明依赖和循环依赖进入主分支。 */
class ModularityTests {

  private final ApplicationModules modules =
      ApplicationModules.of(LingxiCompanionApplication.class);

  @Test
  void shouldRespectDeclaredModuleBoundaries() {
    modules.verify();
  }
}
