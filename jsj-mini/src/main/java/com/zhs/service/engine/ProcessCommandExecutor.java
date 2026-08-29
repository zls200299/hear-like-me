package com.zhs.service.engine;

import com.zhs.config.EngineProperties;
import com.zhs.exception.ServiceException;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStream;
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

    private static final int ERROR_TAIL_MAX_LEN = 1000;

    @Resource
    private EngineProperties engineProperties;

    public ProcessResult execute(String label, List<String> command) {
        int timeoutSeconds = Math.max(engineProperties.getTimeoutSeconds(), 1);
        String commandForLog = sanitizeCommandForLog(command);
        log.info("{} 开始执行, command={}", label, commandForLog);

        long startMs = System.currentTimeMillis();
        ProcessBuilder builder = new ProcessBuilder(command);

        Process process;
        try {
            process = builder.start();
        } catch (Exception e) {
            throw new ServiceException(label + " 启动失败: " + e.getMessage());
        }

        StringBuilder stdout = new StringBuilder();
        StringBuilder stderr = new StringBuilder();
        Thread outReader = startReader(label + "-stdout", process.getInputStream(), stdout);
        Thread errReader = startReader(label + "-stderr", process.getErrorStream(), stderr);

        boolean finished;
        try {
            finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            process.destroyForcibly();
            throw new ServiceException(label + " 被中断");
        }

        awaitReader(outReader);
        awaitReader(errReader);

        long durationMs = System.currentTimeMillis() - startMs;

        if (!finished) {
            process.destroyForcibly();
            log.error("{} 超时, 耗时={}ms, command={}", label, durationMs, commandForLog);
            throw new ServiceException(label + " 超时（" + timeoutSeconds + "s）");
        }

        int exitCode = process.exitValue();
        String stdoutText = stdout.toString().trim();
        String stderrText = stderr.toString().trim();

        log.info("{} 结束, exitCode={}, 耗时={}ms", label, exitCode, durationMs);
        if (!stdoutText.isEmpty()) {
            log.debug("{} stdout 摘要: {}", label, tail(stdoutText, 500));
        }
        if (!stderrText.isEmpty()) {
            log.debug("{} stderr 摘要: {}", label, tail(stderrText, 500));
        }

        if (exitCode != 0) {
            String errSummary = tail(stderrText, ERROR_TAIL_MAX_LEN);
            if (errSummary.isEmpty()) {
                errSummary = tail(stdoutText, ERROR_TAIL_MAX_LEN);
            }
            if (errSummary.isEmpty()) {
                errSummary = "exit code " + exitCode;
            }
            log.error("{} 失败, exitCode={}, 耗时={}ms, stderrTail={}", label, exitCode, durationMs, errSummary);
            throw new ServiceException(label + " 失败: " + errSummary);
        }

        return new ProcessResult(exitCode, stdoutText, stderrText, durationMs);
    }

    private static Thread startReader(String name, InputStream stream, StringBuilder target) {
        Thread thread = new Thread(() -> readStream(stream, target), name);
        thread.setDaemon(true);
        thread.start();
        return thread;
    }

    private static void readStream(InputStream stream, StringBuilder target) {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                target.append(line).append('\n');
            }
        } catch (Exception ignored) {
            // ignore
        }
    }

    private static void awaitReader(Thread reader) {
        try {
            reader.join(5000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
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

    private static String tail(String text, int maxLen) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        if (text.length() <= maxLen) {
            return text;
        }
        return text.substring(text.length() - maxLen);
    }

    public record ProcessResult(int exitCode, String stdout, String stderr, long durationMs) {
    }
}
