package com.lingxi.ai;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import com.lingxi.common.security.annotation.EnableCustomConfig;
import com.lingxi.common.security.annotation.EnableRyFeignClients;

/**
 * AI智能服务
 * 
 * @author lingxi
 */
@EnableCustomConfig
@EnableRyFeignClients(basePackages = "com.lingxi")
@SpringBootApplication(scanBasePackages = {"com.lingxi.ai", "com.lingxi"})
public class LingXiAiApplication {
    public static void main(String[] args) {
        SpringApplication.run(LingXiAiApplication.class, args);
        System.out.println("(♥◠‿◠)ﾉﾞ  AI智能服务启动成功   ლ(´ڡ`ლ)ﾞ");
    }
}
