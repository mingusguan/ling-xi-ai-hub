package com.lingxi.knowledge;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * AI知识库智能问答系统启动类
 *
 * @author comate
 */
@EnableFeignClients(basePackages = "com.lingxi")
@EnableDiscoveryClient
@SpringBootApplication(scanBasePackages = {"com.lingxi.knowledge", "com.lingxi"})
public class LingXiKnowledgeApplication {

    public static void main(String[] args) {
        SpringApplication.run(LingXiKnowledgeApplication.class, args);
        System.out.println("(♥◠‿◠)ノ゙  AI知识库智能问答服务启动成功   ლ(´ڡ`ლ)゙");
    }
}
