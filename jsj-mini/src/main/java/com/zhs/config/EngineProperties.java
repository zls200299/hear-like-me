package com.zhs.config;

import com.zhs.exception.ServiceException;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Hear Like Me 音频引擎配置（Python / FFmpeg）
 */
@Data
@Component
@ConfigurationProperties(prefix = "hear-like-me.engine")
public class EngineProperties {

    private static final String VOCODER_SCRIPT = "cochlear_vocoder.py";
    private static final String REALTIME_VOCODER_SCRIPT = "realtime_vocoder.py";

    /**
     * Python 可执行文件路径
     */
    private String pythonPath = "python";

    /**
     * cochlear_vocoder.py 脚本路径（可为相对路径，相对 JVM 工作目录解析）
     */
    private String scriptPath = "../audio-engine/cochlear_vocoder.py";

    /**
     * realtime_vocoder.py 脚本路径（常驻 Python 实时链路）
     */
    private String realtimeScriptPath = "../audio-engine/realtime_vocoder.py";

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
        return resolveEngineScriptPath(scriptPath, VOCODER_SCRIPT, "声码器脚本不存在，请配置 hear-like-me.engine.script-path。");
    }

    public Path resolveRealtimeScriptPath() {
        return resolveEngineScriptPath(
                realtimeScriptPath,
                REALTIME_VOCODER_SCRIPT,
                "实时声码器脚本不存在，请配置 hear-like-me.engine.realtime-script-path。"
        );
    }

    private Path resolveEngineScriptPath(String configuredScriptPath, String scriptFileName, String errorMessage) {
        Path userDir = Paths.get(System.getProperty("user.dir")).toAbsolutePath().normalize();
        List<Path> candidates = buildScriptCandidates(userDir, configuredScriptPath, scriptFileName);

        for (Path candidate : candidates) {
            if (Files.isRegularFile(candidate)) {
                return candidate.normalize();
            }
        }

        throw new ServiceException(errorMessage + " 已尝试: " + candidates.get(0));
    }

    private List<Path> buildScriptCandidates(Path userDir, String configuredScriptPath, String scriptFileName) {
        Set<String> seen = new LinkedHashSet<>();
        List<Path> candidates = new ArrayList<>();

        addCandidate(candidates, seen, resolveConfiguredPath(userDir, configuredScriptPath));
        addCandidate(candidates, seen, userDir.resolve("audio-engine").resolve(scriptFileName));
        addCandidate(candidates, seen, userDir.resolve("scaffolding-v2/audio-engine").resolve(scriptFileName));
        addCandidate(candidates, seen, userDir.resolve("../audio-engine").resolve(scriptFileName));
        addCandidate(candidates, seen, userDir.resolve("../../audio-engine").resolve(scriptFileName));

        Path parent = userDir.getParent();
        if (parent != null) {
            addCandidate(candidates, seen, parent.resolve("audio-engine").resolve(scriptFileName));
            addCandidate(candidates, seen, parent.resolve("scaffolding-v2/audio-engine").resolve(scriptFileName));
        }

        Path grandParent = parent != null ? parent.getParent() : null;
        if (grandParent != null) {
            addCandidate(candidates, seen, grandParent.resolve("audio-engine").resolve(scriptFileName));
        }

        return candidates;
    }

    private Path resolveConfiguredPath(Path userDir, String configuredScriptPath) {
        if (!StringUtils.hasText(configuredScriptPath)) {
            return null;
        }
        Path configured = Paths.get(configuredScriptPath.trim());
        return configured.isAbsolute() ? configured.normalize() : userDir.resolve(configured).normalize();
    }

    private static void addCandidate(List<Path> candidates, Set<String> seen, Path candidate) {
        if (candidate == null) {
            return;
        }
        Path normalized = candidate.normalize().toAbsolutePath();
        if (seen.add(normalized.toString())) {
            candidates.add(normalized);
        }
    }
}
