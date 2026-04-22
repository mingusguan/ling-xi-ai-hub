package com.lingxi.system;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import com.lingxi.common.security.annotation.EnableCustomConfig;
import com.lingxi.common.security.annotation.EnableRyFeignClients;

/**
 * 系统模块
 * 
 * @author cloud
 */
@EnableCustomConfig
@EnableRyFeignClients
@SpringBootApplication
public class LingXiSystemApplication
{
    public static void main(String[] args)
    {
        SpringApplication.run(LingXiSystemApplication.class, args);
        System.out.println("(♥◠‿◠)ﾉﾞ  系统模块启动成功   ლ(´ڡ`ლ)ﾞ  \n" +
                " .-------.       ____     __        \n" +
                " |  _ _   \\      \\   \\   /  /    \n" +
                " | ( ' )  |       \\  _. /  '       \n" +
                " |(_ o _) /        _( )_ .'         \n" +
                " | (_,_).' __  ___(_ o _)'          \n" +
                " |  |\\ \\  |  ||   |(_,_)'         \n" +
                " |  | \\ `'   /|   `-'  /           \n" +
                " |  |  \\    /  \\      /           \n" +
                " ''-'   `'-'    `-..-'              ");
    }
}
