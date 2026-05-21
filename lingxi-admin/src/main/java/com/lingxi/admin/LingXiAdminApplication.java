package com.lingxi.admin;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * LingXi AI Hub 单体启动入口。
 */
@EnableScheduling
@MapperScan("com.lingxi.**.mapper")
@SpringBootApplication
@ComponentScan(
        basePackages = "com.lingxi",
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.REGEX,
                pattern = {
                        "com\\.lingxi\\.auth\\.LingXiAuthApplication",
                        "com\\.lingxi\\.system\\.LingXiSystemApplication",
                        "com\\.lingxi\\.gen\\.LingXiGenApplication",
                        "com\\.lingxi\\.job\\.LingXiJobApplication",
                        "com\\.lingxi\\.file\\.LingXiFileApplication",
                        "com\\.lingxi\\.knowledge\\.LingXiKnowledgeApplication",
                        "com\\.lingxi\\.oa\\.LingXiOaApplication",
                        "com\\.lingxi\\.ai\\.LingXiAiApplication"
                }
        )
)
public class LingXiAdminApplication {

    public static void main(String[] args) {
        SpringApplication.run(LingXiAdminApplication.class, args);
        System.out.println("LingXi AI Hub started successfully.");
    }
}
