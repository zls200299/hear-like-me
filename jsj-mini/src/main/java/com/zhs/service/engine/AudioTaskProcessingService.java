package com.zhs.service.engine;

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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 音频处理任务编排：FFmpeg 标准化 + Python 声码器
 */
@Slf4j
@Service
public class AudioTaskProcessingService {

    private static final String DEFAULT_OWNER = "guest";
    private static final String ALGORITHM_VERSION = "cochlear-vocoder-v1";

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

        try {
            Path sourcePath = localFileStorageService.resolvePath(sourceAsset.getObjectKey());

            NormalizeResult normalizeResult = ffmpegNormalizeService.normalize(sourcePath, sourceAsset, DEFAULT_OWNER);
            task.setNormalizedAssetId(normalizeResult.getAsset().getId());
            task.setProgress(40);
            audioProcessingTaskService.updateById(task);

            String outputFilename = UUID.randomUUID().toString().replace("-", "") + ".wav";
            String outputObjectKey = localFileStorageService.buildAudioOutputObjectKey(DEFAULT_OWNER, outputFilename);
            localFileStorageService.createParentDirectories(outputObjectKey);
            Path outputPath = localFileStorageService.resolvePath(outputObjectKey);

            VocoderParams vocoderParams = buildVocoderParams(req);
            VocoderResult vocoderResult = cochlearVocoderEngineService.process(
                    normalizeResult.getLocalPath(), outputPath, vocoderParams);

            FileAsset outputAsset = buildOutputAsset(sourceAsset, outputObjectKey, vocoderResult.getOutputPath());
            fileAssetService.save(outputAsset);

            String clarityGrade = StringUtils.hasText(vocoderResult.getClarityGrade())
                    ? vocoderResult.getClarityGrade()
                    : "模拟完成";

            task.setOutputAssetId(outputAsset.getId());
            task.setTaskStatus("SUCCESS");
            task.setProgress(100);
            task.setClarityScore(vocoderResult.getClarityScore());
            task.setClarityGrade(clarityGrade);
            task.setProcessingFinishedTime(new Date());
            audioProcessingTaskService.updateById(task);

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("taskNo", taskNo);
            result.put("status", "SUCCESS");
            result.put("outputAssetId", String.valueOf(outputAsset.getId()));
            result.put("processedAudioUrl", localFileStorageService.buildPreviewUrl(outputAsset.getId()));
            result.put("clarityScore", vocoderResult.getClarityScore());
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
    }

    private AudioProcessingTask buildTask(AudioTaskCreateReq req, String taskNo, Date now) {
        AudioProcessingTask task = new AudioProcessingTask();
        task.setTaskNo(taskNo);
        task.setUserId(null);
        task.setSourceType(req.getSourceType() != null ? req.getSourceType() : "UPLOAD");
        task.setSourceAssetId(req.getSourceAssetId());
        task.setScenarioCode(req.getScenarioCode());
        task.setNChannels(req.getNChannels() != null ? req.getNChannels() : 8);
        task.setCarrier(StringUtils.hasText(req.getCarrier()) ? req.getCarrier() : "noise");
        task.setFLo(req.getFLo() != null ? req.getFLo() : BigDecimal.valueOf(150));
        task.setFHi(req.getFHi() != null ? req.getFHi() : BigDecimal.valueOf(7000));
        task.setEnvCut(req.getEnvCut() != null ? req.getEnvCut() : BigDecimal.valueOf(160));
        task.setSpread(req.getSpread() != null ? req.getSpread() : BigDecimal.ZERO);
        task.setNoiseLevel(req.getNoiseLevel() != null ? req.getNoiseLevel() : BigDecimal.ZERO);
        task.setAlgorithmVersion(ALGORITHM_VERSION);
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
        return message.length() > 1000 ? message.substring(0, 1000) : message;
    }

    private String generateTaskNo() {
        String timePart = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        int randomPart = ThreadLocalRandom.current().nextInt(1000);
        return "HLK" + timePart + String.format("%03d", randomPart);
    }
}
