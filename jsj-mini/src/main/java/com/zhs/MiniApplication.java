package com.zhs;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 小程序后端服务启动类
 */
@SpringBootApplication
public class MiniApplication {

    public static void main(String[] args) {
        SpringApplication.run(MiniApplication.class, args);
        System.out.println("(♥◠‿◠)ノ゛  小程序后端服务启动成功   ლ(´ڡ`ლ)゛");
    }
}
