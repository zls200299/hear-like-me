package com.zhs.service.engine;

import com.zhs.config.EngineProperties;
import com.zhs.exception.ServiceException;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 调用 Python cochlear_vocoder.py 进行人工耳蜗声码器处理
 */
@Slf4j
@Service
public class CochlearVocoderEngineService {

    private static final Pattern CLARITY_SCORE_ASCII =
            Pattern.compile("CLARITY_SCORE=(\\d+)", Pattern.MULTILINE);
    private static final Pattern CLARITY_PATTERN =
            Pattern.compile("可懂度估分:\\s*(\\d+)/100\\s*\\(([^)]+)\\)");

    @Resource
    private EngineProperties engineProperties;

    @Resource
    private ProcessCommandExecutor processCommandExecutor;

    public VocoderResult process(Path inputWavPath, Path outputWavPath, VocoderParams params) {
        if (inputWavPath == null || !Files.exists(inputWavPath)) {
            throw new ServiceException("标准化 WAV 不存在");
        }
        if (outputWavPath == null) {
            throw new ServiceException("输出路径无效");
        }

        Path scriptPath = engineProperties.resolveScriptPath();
        if (!Files.exists(scriptPath)) {
            throw new ServiceException("声码器脚本不存在: " + scriptPath);
        }

        try {
            Path parent = outputWavPath.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
        } catch (Exception e) {
            throw new ServiceException("创建输出目录失败: " + e.getMessage());
        }

        List<String> command = buildCommand(scriptPath, inputWavPath, outputWavPath, params);
        ProcessCommandExecutor.ProcessResult processResult =
                processCommandExecutor.execute("Cochlear Vocoder", command);

        log.info("Cochlear Vocoder 完成, exitCode={}, 耗时={}ms",
                processResult.exitCode(), processResult.durationMs());

        if (!Files.exists(outputWavPath) || fileSize(outputWavPath) <= 0) {
            throw new ServiceException("声码器输出文件无效");
        }

        VocoderResult result = new VocoderResult();
        result.setOutputPath(outputWavPath);
        if (!parseClarity(processResult.stdout(), result)) {
            log.warn("Cochlear Vocoder 未解析到可懂度估分，stdout 摘要: {}",
                    tailForLog(processResult.stdout()));
        }
        return result;
    }

    private static String tailForLog(String text) {
        if (!StringUtils.hasText(text)) {
            return "";
        }
        String trimmed = text.trim();
        if (trimmed.length() <= 300) {
            return trimmed;
        }
        return trimmed.substring(trimmed.length() - 300);
    }

    private List<String> buildCommand(Path scriptPath, Path inputWavPath, Path outputWavPath,
                                      VocoderParams params) {
        List<String> command = new ArrayList<>();
        command.add(engineProperties.getPythonPath());
        command.add(scriptPath.toString());
        command.add("--input");
        command.add(inputWavPath.toString());
        command.add("--output");
        command.add(outputWavPath.toString());

        if (StringUtils.hasText(params.getScenarioCode())) {
            command.add("--scenario");
            command.add(params.getScenarioCode().trim());
        }

        command.add("--channels");
        command.add(String.valueOf(params.getNChannels()));

        if (StringUtils.hasText(params.getCarrier())) {
            command.add("--carrier");
            command.add(params.getCarrier().trim());
        }

        command.add("--lo");
        command.add(stripDecimal(params.getFLo()));
        command.add("--hi");
        command.add(stripDecimal(params.getFHi()));
        command.add("--env-cut");
        command.add(stripDecimal(params.getEnvCut()));
        command.add("--spread");
        command.add(stripDecimal(toPercent(params.getSpread())));
        command.add("--noise");
        command.add(stripDecimal(toPercent(params.getNoiseLevel())));

        return command;
    }

    private static BigDecimal toPercent(BigDecimal ratio) {
        if (ratio == null) {
            return BigDecimal.ZERO;
        }
        return ratio.multiply(BigDecimal.valueOf(100));
    }

    private static String stripDecimal(BigDecimal value) {
        if (value == null) {
            return "0";
        }
        return value.stripTrailingZeros().toPlainString();
    }

    private static long fileSize(Path path) {
        try {
            return Files.size(path);
        } catch (Exception e) {
            return 0L;
        }
    }

    private static boolean parseClarity(String output, VocoderResult result) {
        if (!StringUtils.hasText(output)) {
            return false;
        }

        Matcher asciiMatcher = CLARITY_SCORE_ASCII.matcher(output);
        if (asciiMatcher.find()) {
            try {
                int score = Integer.parseInt(asciiMatcher.group(1));
                result.setClarityScore(score);
                result.setClarityGrade(gradeFromScore(score));
                return true;
            } catch (NumberFormatException ignored) {
                // fall through
            }
        }

        Matcher matcher = CLARITY_PATTERN.matcher(output);
        if (!matcher.find()) {
            return false;
        }
        try {
            result.setClarityScore(Integer.parseInt(matcher.group(1)));
        } catch (NumberFormatException ignored) {
            return false;
        }
        result.setClarityGrade(matcher.group(2).trim());
        return true;
    }

    private static String gradeFromScore(int score) {
        if (score < 24) {
            return "几乎听不懂";
        }
        if (score < 44) {
            return "很吃力";
        }
        if (score < 66) {
            return "大致能懂";
        }
        if (score < 86) {
            return "比较清楚";
        }
        return "接近清晰";
    }
}
