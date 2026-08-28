package com.zhs.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 微信小程序配置。与 application.yml 中 wx.mini 下 appId / appSecret 对应（亦支持 app-id / app-secret 写法）。
 */
@Data
@Component
@ConfigurationProperties(prefix = "wx.mini")
public class WxMiniProperties {

    private String appId;
    private String appSecret;
}
