package com.lingxi;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.modulith.Modulithic;
import org.springframework.scheduling.annotation.EnableScheduling;

/** 灵犀伴行模块化单体的唯一启动入口。 */
@EnableScheduling
@Modulithic(systemName = "LingXi Companion", sharedModules = "kernel")
@MapperScan({
  "com.lingxi.platform.persistence",
  "com.lingxi.identity.infrastructure.persistence",
  "com.lingxi.goal.infrastructure.persistence",
  "com.lingxi.companion.infrastructure.persistence",
  "com.lingxi.relationship.infrastructure.persistence",
  "com.lingxi.engagement.infrastructure.persistence",
  "com.lingxi.content.infrastructure.persistence",
  "com.lingxi.commerce.infrastructure.persistence",
  "com.lingxi.operations.infrastructure.persistence"
})
@SpringBootApplication
public class LingxiCompanionApplication {

  public static void main(String[] args) {
    SpringApplication.run(LingxiCompanionApplication.class, args);
  }
}
