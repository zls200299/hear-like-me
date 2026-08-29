package com.zhs.service.engine;

import com.zhs.config.EngineProperties;
import com.zhs.exception.ServiceException;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 外部命令执行器（FFmpeg / Python）
 */
@Slf4j
@Component
public class ProcessCommandExecutor {

    @Resource
    private EngineProperties engineProperties;

    public ProcessResult execute(String label, List<String> command) {
        int timeoutSeconds = Math.max(engineProperties.getTimeoutSeconds(), 1);
        log.info("{} 开始执行: {}", label, sanitizeCommandForLog(command));

        ProcessBuilder builder = new ProcessBuilder(command);
        builder.redirectErrorStream(true);

        Process process;
        try {
            process = builder.start();
        } catch (Exception e) {
            throw new ServiceException(label + " 启动失败: " + e.getMessage());
        }

        StringBuilder output = new StringBuilder();
        Thread reader = new Thread(() -> {
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = br.readLine()) != null) {
                    output.append(line).append('\n');
                }
            } catch (Exception e) {
                log.warn("{} 读取输出失败: {}", label, e.getMessage());
            }
        }, label + "-stdout-reader");
        reader.setDaemon(true);
        reader.start();

        boolean finished;
        try {
            finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            process.destroyForcibly();
            throw new ServiceException(label + " 被中断");
        }

        try {
            reader.join(3000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        if (!finished) {
            process.destroyForcibly();
            throw new ServiceException(label + " 超时（" + timeoutSeconds + "s）");
        }

        int exitCode = process.exitValue();
        String text = output.toString().trim();
        if (exitCode != 0) {
            String detail = text.isEmpty() ? "exit code " + exitCode : text;
            throw new ServiceException(label + " 失败: " + truncate(detail, 800));
        }

        log.info("{} 执行成功", label);
        return new ProcessResult(exitCode, text);
    }

    private static String sanitizeCommandForLog(List<String> command) {
        if (command == null || command.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < command.size(); i++) {
            if (i > 0) {
                sb.append(' ');
            }
            String arg = command.get(i);
            if (i > 0 && looksLikePath(arg)) {
                sb.append("<path>");
            } else {
                sb.append(arg);
            }
        }
        return sb.toString();
    }

    private static boolean looksLikePath(String arg) {
        if (arg == null || arg.isEmpty()) {
            return false;
        }
        return arg.contains(":\\") || arg.contains("/") || arg.contains("\\");
    }

    private static String truncate(String text, int maxLen) {
        if (text == null) {
            return "";
        }
        if (text.length() <= maxLen) {
            return text;
        }
        return text.substring(0, maxLen) + "...";
    }

    public record ProcessResult(int exitCode, String output) {
    }
}
