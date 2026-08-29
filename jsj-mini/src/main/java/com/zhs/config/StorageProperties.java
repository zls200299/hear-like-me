package com.zhs.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Hear Like Me 文件存储配置
 */
@Data
@Component
@ConfigurationProperties(prefix = "hear-like-me.storage")
public class StorageProperties {

    /**
     * 存储提供方，当前仅支持 LOCAL
     */
    private String provider = "LOCAL";

    /**
     * 本地存储根目录（绝对路径）
     */
    private String rootPath;

    /**
     * 文件预览 URL 前缀，如 http://localhost:8081
     */
    private String previewUrlPrefix;
}
