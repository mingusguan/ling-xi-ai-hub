package com.lingxi.admin;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * LingXi AI Hub 单体启动入口。
 */
@EnableScheduling
@EnableAspectJAutoProxy(exposeProxy = true)
@MapperScan("com.lingxi.**.mapper")
@SpringBootApplication(scanBasePackages = "com.lingxi")
public class LingXiAdminApplication {

    public static void main(String[] args) {
        SpringApplication.run(LingXiAdminApplication.class, args);
        System.out.println("LingXi AI Hub started successfully.");
    }
}
