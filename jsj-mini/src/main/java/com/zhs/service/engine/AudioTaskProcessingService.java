package com.zhs.service.engine;

import com.zhs.config.EngineProperties;
import com.zhs.exception.ServiceException;
import com.zhs.model.AudioProcessingTask;
import com.zhs.model.FileAsset;
import com.zhs.request.AudioTaskCreateReq;
import com.zhs.service.IAudioProcessingTaskService;
import com.zhs.service.IFileAssetService;
import com.zhs.service.storage.LocalFileStorageService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 音频处理任务编排：FFmpeg 标准化 +（可选）Python 声码器
 */
@Slf4j
@Service
public class AudioTaskProcessingService {

    private static final String DEFAULT_OWNER = "guest";
    private static final String ALGORITHM_FFMPEG_ONLY = "ffmpeg-normalize-v1";
    private static final String ALGORITHM_VOCODER = "cochlear-vocoder-v1";

    @Resource
    private EngineProperties engineProperties;

    @Resource
    private IFileAssetService fileAssetService;

    @Resource
    private IAudioProcessingTaskService audioProcessingTaskService;

    @Resource
    private LocalFileStorageService localFileStorageService;

    @Resource
    private FFmpegNormalizeService ffmpegNormalizeService;

    @Resource
    private CochlearVocoderEngineService cochlearVocoderEngineService;

    public Map<String, Object> createAndProcess(AudioTaskCreateReq req) {
        if (req.getSourceAssetId() == null) {
            throw new ServiceException("sourceAssetId 不能为空");
        }

        FileAsset sourceAsset = fileAssetService.getById(req.getSourceAssetId());
        if (sourceAsset == null) {
            throw new ServiceException("源文件不存在");
        }
        if (!localFileStorageService.exists(sourceAsset.getObjectKey())) {
            throw new ServiceException("源文件不存在");
        }

        Date now = new Date();
        String taskNo = generateTaskNo();

        AudioProcessingTask task = buildTask(req, taskNo, now);
        audioProcessingTaskService.save(task);

        log.info("任务开始 taskNo={}, sourceObjectKey={}", taskNo, sourceAsset.getObjectKey());

        try {
            Path sourcePath = localFileStorageService.resolvePath(sourceAsset.getObjectKey());
            log.info("任务 taskNo={}, 输入文件路径={}", taskNo, sourcePath);

            NormalizeResult normalizeResult = ffmpegNormalizeService.normalize(sourcePath, sourceAsset, DEFAULT_OWNER);
            task.setNormalizedAssetId(normalizeResult.getAsset().getId());
            task.setProgress(40);
            audioProcessingTaskService.updateById(task);

            log.info("任务 taskNo={}, normalizedAssetId={}, normalizedObjectKey={}",
                    taskNo, normalizeResult.getAsset().getId(), normalizeResult.getAsset().getObjectKey());

            String outputFilename = UUID.randomUUID().toString().replace("-", "") + ".wav";
            String outputObjectKey = localFileStorageService.buildAudioOutputObjectKey(DEFAULT_OWNER, outputFilename);
            localFileStorageService.createParentDirectories(outputObjectKey);
            Path outputPath = localFileStorageService.resolvePath(outputObjectKey);

            log.info("任务 taskNo={}, outputObjectKey={}", taskNo, outputObjectKey);

            Integer clarityScore = null;
            String clarityGrade;

            if (engineProperties.isVocoderEnabled()) {
                task.setAlgorithmVersion(ALGORITHM_VOCODER);
                VocoderParams vocoderParams = buildVocoderParams(req);
                VocoderResult vocoderResult = cochlearVocoderEngineService.process(
                        normalizeResult.getLocalPath(), outputPath, vocoderParams);
                clarityScore = vocoderResult.getClarityScore();
                clarityGrade = StringUtils.hasText(vocoderResult.getClarityGrade())
                        ? vocoderResult.getClarityGrade()
                        : "模拟完成";
            } else {
                task.setAlgorithmVersion(ALGORITHM_FFMPEG_ONLY);
                Files.copy(normalizeResult.getLocalPath(), outputPath, StandardCopyOption.REPLACE_EXISTING);
                clarityGrade = "模拟完成";
                log.info("任务 taskNo={}, vocoder 未启用，已复制 normalized wav 为 output", taskNo);
            }

            if (!Files.exists(outputPath) || fileSize(outputPath) <= 0) {
                throw new ServiceException("输出文件无效");
            }

            FileAsset outputAsset = buildOutputAsset(sourceAsset, outputObjectKey, outputPath);
            fileAssetService.save(outputAsset);

            task.setOutputAssetId(outputAsset.getId());
            task.setTaskStatus("SUCCESS");
            task.setProgress(100);
            task.setClarityScore(clarityScore);
            task.setClarityGrade(clarityGrade);
            task.setProcessingFinishedTime(new Date());
            audioProcessingTaskService.updateById(task);

            log.info("任务成功 taskNo={}, outputAssetId={}, outputObjectKey={}",
                    taskNo, outputAsset.getId(), outputObjectKey);

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("taskNo", taskNo);
            result.put("status", "SUCCESS");
            result.put("outputAssetId", String.valueOf(outputAsset.getId()));
            result.put("processedAudioUrl", localFileStorageService.buildPreviewUrl(outputAsset.getId()));
            result.put("clarityScore", clarityScore);
            result.put("clarityGrade", clarityGrade);
            return result;
        } catch (Exception e) {
            log.error("音频任务处理失败 taskNo={}", taskNo, e);
            markTaskFailed(task, e);
            if (e instanceof ServiceException serviceException) {
                throw serviceException;
            }
            throw new ServiceException("音频处理失败: " + e.getMessage());
        }
    }

    private void markTaskFailed(AudioProcessingTask task, Exception e) {
        task.setTaskStatus("FAILED");
        task.setProgress(0);
        task.setErrorMessage(truncateError(e.getMessage()));
        task.setProcessingFinishedTime(new Date());
        audioProcessingTaskService.updateById(task);
        log.warn("任务失败 taskNo={}, error={}", task.getTaskNo(), task.getErrorMessage());
    }

    private AudioProcessingTask buildTask(AudioTaskCreateReq req, String taskNo, Date now) {
        AudioProcessingTask task = new AudioProcessingTask();
        task.setTaskNo(taskNo);
        task.setUserId(null);
        task.setSourceType(req.getSourceType() != null ? req.getSourceType() : "UPLOAD");
        task.setSourceAssetId(req.getSourceAssetId());
        if (StringUtils.hasText(req.getSampleCode())) {
            task.setSampleCode(req.getSampleCode());
        }
        task.setScenarioCode(req.getScenarioCode());
        task.setNChannels(req.getNChannels() != null ? req.getNChannels() : 8);
        task.setCarrier(StringUtils.hasText(req.getCarrier()) ? req.getCarrier() : "noise");
        task.setFLo(req.getFLo() != null ? req.getFLo() : BigDecimal.valueOf(150));
        task.setFHi(req.getFHi() != null ? req.getFHi() : BigDecimal.valueOf(7000));
        task.setEnvCut(req.getEnvCut() != null ? req.getEnvCut() : BigDecimal.valueOf(160));
        task.setSpread(req.getSpread() != null ? req.getSpread() : BigDecimal.ZERO);
        task.setNoiseLevel(req.getNoiseLevel() != null ? req.getNoiseLevel() : BigDecimal.ZERO);
        task.setAlgorithmVersion(engineProperties.isVocoderEnabled() ? ALGORITHM_VOCODER : ALGORITHM_FFMPEG_ONLY);
        task.setTaskStatus("PROCESSING");
        task.setProgress(0);
        task.setRetryCount(0);
        task.setProcessingStartedTime(now);
        return task;
    }

    private VocoderParams buildVocoderParams(AudioTaskCreateReq req) {
        VocoderParams params = new VocoderParams();
        params.setNChannels(req.getNChannels() != null ? req.getNChannels() : 8);
        params.setCarrier(StringUtils.hasText(req.getCarrier()) ? req.getCarrier() : "noise");
        params.setFLo(req.getFLo() != null ? req.getFLo() : BigDecimal.valueOf(150));
        params.setFHi(req.getFHi() != null ? req.getFHi() : BigDecimal.valueOf(7000));
        params.setEnvCut(req.getEnvCut() != null ? req.getEnvCut() : BigDecimal.valueOf(160));
        params.setSpread(req.getSpread() != null ? req.getSpread() : BigDecimal.ZERO);
        params.setNoiseLevel(req.getNoiseLevel() != null ? req.getNoiseLevel() : BigDecimal.ZERO);
        params.setScenarioCode(req.getScenarioCode());
        return params;
    }

    private FileAsset buildOutputAsset(FileAsset sourceAsset, String outputObjectKey, Path outputPath) {
        FileAsset outputAsset = new FileAsset();
        outputAsset.setOwnerUserId(null);
        outputAsset.setParentAssetId(sourceAsset.getId());
        outputAsset.setAssetType("AUDIO_OUTPUT");
        outputAsset.setStorageProvider("LOCAL");
        outputAsset.setBucketName("");
        outputAsset.setObjectKey(outputObjectKey);
        outputAsset.setOriginalFilename(resolveOutputFilename(sourceAsset.getOriginalFilename()));
        outputAsset.setFileExt("wav");
        outputAsset.setMimeType("audio/wav");
        outputAsset.setFileSize(fileSize(outputPath));
        outputAsset.setAccessMode("PRIVATE");
        outputAsset.setStatus("ACTIVE");
        return outputAsset;
    }

    private static String resolveOutputFilename(String originalFilename) {
        if (!StringUtils.hasText(originalFilename)) {
            return "processed.wav";
        }
        int dot = originalFilename.lastIndexOf('.');
        String base = dot > 0 ? originalFilename.substring(0, dot) : originalFilename;
        return base + "_processed.wav";
    }

    private static long fileSize(Path path) {
        try {
            return Files.size(path);
        } catch (Exception e) {
            return 0L;
        }
    }

    private static String truncateError(String message) {
        if (!StringUtils.hasText(message)) {
            return "未知错误";
        }
        if (message.length() <= 1000) {
            return message;
        }
        return message.substring(message.length() - 1000);
    }

    private String generateTaskNo() {
        String timePart = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        int randomPart = ThreadLocalRandom.current().nextInt(1000);
        return "HLK" + timePart + String.format("%03d", randomPart);
    }
}
