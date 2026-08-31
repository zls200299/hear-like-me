package com.zhs;

import com.zhs.config.EngineProperties;
import com.zhs.config.StorageProperties;
import com.zhs.config.WxMiniProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * 小程序后端服务启动类
 */
@SpringBootApplication
@EnableConfigurationProperties({
        StorageProperties.class,
        EngineProperties.class,
        WxMiniProperties.class
})
public class MiniApplication {

    public static void main(String[] args) {
        SpringApplication.run(MiniApplication.class, args);
        System.out.println("(♥◠‿◠)ノ゛  小程序后端服务启动成功   ლ(´ڡ`ლ)゛");
    }
}
