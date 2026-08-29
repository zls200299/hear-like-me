package com.zhs.config;

import com.zhs.exception.ServiceException;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Hear Like Me 音频引擎配置（Python / FFmpeg）
 */
@Data
@Component
@ConfigurationProperties(prefix = "hear-like-me.engine")
public class EngineProperties {

    /**
     * Python 可执行文件路径
     */
    private String pythonPath = "python";

    /**
     * cochlear_vocoder.py 脚本路径（可为相对路径，相对 JVM 工作目录解析）
     */
    private String scriptPath = "../audio-engine/cochlear_vocoder.py";

    /**
     * 子进程超时（秒）
     */
    private int timeoutSeconds = 120;

    /**
     * FFmpeg 可执行文件路径
     */
    private String ffmpegPath = "ffmpeg";

    /**
     * 是否调用 Python 声码器；false 时仅 FFmpeg 标准化后复制为 output（用于联调 FFmpeg）
     */
    private boolean vocoderEnabled = true;

    public Path resolveScriptPath() {
        if (!StringUtils.hasText(scriptPath)) {
            throw new ServiceException("hear-like-me.engine.script-path 未配置");
        }
        Path configured = Paths.get(scriptPath.trim());
        if (configured.isAbsolute()) {
            return configured.normalize();
        }

        Path fromCwd = Paths.get(System.getProperty("user.dir")).resolve(configured).normalize();
        if (Files.exists(fromCwd)) {
            return fromCwd;
        }

        Path fromRepoRoot = Paths.get(System.getProperty("user.dir"))
                .resolve("../../audio-engine/cochlear_vocoder.py")
                .normalize();
        if (Files.exists(fromRepoRoot)) {
            return fromRepoRoot;
        }

        return fromCwd;
    }
}
