package com.lingxi.oa;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableFeignClients(basePackages = "com.lingxi")
@EnableDiscoveryClient
@EnableScheduling
@SpringBootApplication(scanBasePackages = {"com.lingxi.oa", "com.lingxi"})
public class LingXiOaApplication {

    public static void main(String[] args) {
        SpringApplication.run(LingXiOaApplication.class, args);
        System.out.println("(♥◠‿◠)ノ゙  OA服务启动成功   ლ(´ڡ`ლ)゙");
    }
}
