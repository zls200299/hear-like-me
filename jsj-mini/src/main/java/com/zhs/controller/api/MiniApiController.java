package com.zhs.controller.api;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.zhs.common.NoLoginRequest;
import com.zhs.exception.ServiceException;
import com.zhs.model.AudioProcessingTask;
import com.zhs.model.FileAsset;
import com.zhs.model.SampleAudio;
import com.zhs.model.ScenarioPreset;
import com.zhs.request.AudioTaskCreateReq;
import com.zhs.service.IAudioProcessingTaskService;
import com.zhs.service.IFileAssetService;
import com.zhs.service.ISampleAudioService;
import com.zhs.service.IScenarioPresetService;
import com.zhs.service.storage.LocalFileStorageService;
import com.zhs.util.R;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 小程序业务 API（不登录，一期直接调）
 *
 * @author
 * @since 2026-08-28
 */
@RestController
@RequestMapping("/api")
@NoLoginRequest
@Slf4j
public class MiniApiController {

    private static final String DEFAULT_OWNER = "guest";
    private static final Set<String> ALLOWED_AUDIO_EXT = Set.of("mp3", "wav", "m4a", "aac");

    @Resource
    private IScenarioPresetService scenarioPresetService;

    @Resource
    private ISampleAudioService sampleAudioService;

    @Resource
    private IFileAssetService fileAssetService;

    @Resource
    private IAudioProcessingTaskService audioProcessingTaskService;

    @Resource
    private LocalFileStorageService localFileStorageService;

    // ==================== 1. 场景列表 ====================

    @GetMapping("/scenarios")
    public R<List<ScenarioPreset>> getScenarios() {
        LambdaQueryWrapper<ScenarioPreset> wrapper = new QueryWrapper<ScenarioPreset>().lambda()
                .eq(ScenarioPreset::getEnabled, 1)
                .eq(ScenarioPreset::getIsDelete, 0)
                .orderByAsc(ScenarioPreset::getSortOrder);
        return R.ok(scenarioPresetService.list(wrapper));
    }

    // ==================== 2. 示例音列表 ====================

    @GetMapping("/samples")
    public R<List<SampleAudio>> getSamples() {
        LambdaQueryWrapper<SampleAudio> wrapper = new QueryWrapper<SampleAudio>().lambda()
                .eq(SampleAudio::getEnabled, 1)
                .eq(SampleAudio::getIsDelete, 0)
                .orderByAsc(SampleAudio::getSortOrder);
        return R.ok(sampleAudioService.list(wrapper));
    }

    // ==================== 3. 上传音频文件 ====================

    @PostMapping("/files/audio")
    public R<Map<String, Object>> uploadAudio(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ServiceException("文件不能为空");
        }

        String originalFilename = file.getOriginalFilename();
        String ext = normalizeExtension(StringUtils.getFilenameExtension(originalFilename));
        validateAudioExtension(ext);

        String objectKey = localFileStorageService.buildAudioInputObjectKey(DEFAULT_OWNER, ext);
        localFileStorageService.saveMultipartFile(file, objectKey);

        FileAsset asset = new FileAsset();
        asset.setOwnerUserId(null);
        asset.setAssetType("AUDIO_SOURCE");
        asset.setStorageProvider("LOCAL");
        asset.setBucketName("");
        asset.setObjectKey(objectKey);
        asset.setOriginalFilename(originalFilename);
        asset.setFileExt(ext);
        asset.setMimeType(file.getContentType());
        asset.setFileSize(file.getSize());
        asset.setAccessMode("PRIVATE");
        asset.setStatus("ACTIVE");
        fileAssetService.save(asset);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("assetId", toStringId(asset.getId()));
        result.put("fileName", originalFilename);
        result.put("url", localFileStorageService.buildPreviewUrl(asset.getId()));
        result.put("objectKey", objectKey);
        return R.ok(result);
    }

    // ==================== 4. 文件预览 ====================

    @GetMapping("/files/preview/{assetId}")
    public ResponseEntity<FileSystemResource> previewFile(@PathVariable("assetId") Long assetId) {
        FileAsset asset = fileAssetService.getById(assetId);
        if (asset == null || (asset.getIsDelete() != null && asset.getIsDelete() == 1)) {
            throw new ServiceException("文件不存在");
        }
        if (!StringUtils.hasText(asset.getObjectKey())) {
            throw new ServiceException("文件路径无效");
        }
        if (!localFileStorageService.exists(asset.getObjectKey())) {
            throw new ServiceException("文件不存在");
        }

        Path filePath = localFileStorageService.resolvePath(asset.getObjectKey());
        FileSystemResource resource = new FileSystemResource(filePath);
        String contentType = resolveContentType(asset);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .body(resource);
    }

    // ==================== 5. 创建音频处理任务 ====================

    @PostMapping("/audio/tasks")
    @Transactional
    public R<Map<String, Object>> createTask(@RequestBody AudioTaskCreateReq req) {
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

        String taskNo = generateTaskNo();
        String ext = sourceAsset.getFileExt() != null ? sourceAsset.getFileExt() : "wav";
        String outputFilename = UUID.randomUUID().toString().replace("-", "") + "." + ext;
        String outputObjectKey = localFileStorageService.buildAudioOutputObjectKey(DEFAULT_OWNER, outputFilename);

        Date now = new Date();

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
        task.setAlgorithmVersion("mock-copy-v1");
        task.setTaskStatus("PROCESSING");
        task.setProgress(0);
        task.setRetryCount(0);
        task.setProcessingStartedTime(now);
        audioProcessingTaskService.save(task);

        localFileStorageService.copy(sourceAsset.getObjectKey(), outputObjectKey);

        FileAsset outputAsset = new FileAsset();
        outputAsset.setOwnerUserId(null);
        outputAsset.setParentAssetId(sourceAsset.getId());
        outputAsset.setAssetType("AUDIO_OUTPUT");
        outputAsset.setStorageProvider("LOCAL");
        outputAsset.setBucketName("");
        outputAsset.setObjectKey(outputObjectKey);
        outputAsset.setOriginalFilename(sourceAsset.getOriginalFilename());
        outputAsset.setFileExt(ext);
        outputAsset.setMimeType(sourceAsset.getMimeType());
        try {
            outputAsset.setFileSize(Files.size(localFileStorageService.resolvePath(outputObjectKey)));
        } catch (Exception e) {
            outputAsset.setFileSize(sourceAsset.getFileSize());
        }
        outputAsset.setAccessMode("PRIVATE");
        outputAsset.setStatus("ACTIVE");
        fileAssetService.save(outputAsset);

        task.setOutputAssetId(outputAsset.getId());
        task.setTaskStatus("SUCCESS");
        task.setProgress(100);
        task.setClarityScore(null);
        task.setClarityGrade("模拟完成");
        task.setProcessingFinishedTime(now);
        audioProcessingTaskService.updateById(task);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("taskNo", taskNo);
        result.put("status", "SUCCESS");
        result.put("outputAssetId", toStringId(outputAsset.getId()));
        result.put("processedAudioUrl", localFileStorageService.buildPreviewUrl(outputAsset.getId()));
        result.put("clarityScore", null);
        result.put("clarityGrade", "模拟完成");
        return R.ok(result);
    }

    @GetMapping("/audio/tasks/{taskNo}")
    public R<Map<String, Object>> getTaskDetail(@PathVariable("taskNo") String taskNo) {
        AudioProcessingTask task = getTaskByNo(taskNo);
        return R.ok(buildTaskResponse(task));
    }

    private AudioProcessingTask getTaskByNo(String taskNo) {
        LambdaQueryWrapper<AudioProcessingTask> wrapper = new QueryWrapper<AudioProcessingTask>().lambda()
                .eq(AudioProcessingTask::getTaskNo, taskNo)
                .eq(AudioProcessingTask::getIsDelete, 0);
        AudioProcessingTask task = audioProcessingTaskService.getOne(wrapper);
        if (task == null) {
            throw new ServiceException("任务不存在");
        }
        return task;
    }

    private Map<String, Object> buildTaskResponse(AudioProcessingTask task) {
        String processedAudioUrl = null;
        if (task.getOutputAssetId() != null) {
            processedAudioUrl = localFileStorageService.buildPreviewUrl(task.getOutputAssetId());
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("taskNo", task.getTaskNo());
        result.put("status", task.getTaskStatus());
        result.put("sourceAssetId", toStringId(task.getSourceAssetId()));
        result.put("outputAssetId", toStringId(task.getOutputAssetId()));
        result.put("processedAudioUrl", processedAudioUrl);
        result.put("clarityScore", task.getClarityScore());
        result.put("clarityGrade", task.getClarityGrade());
        result.put("errorMessage", task.getErrorMessage());
        return result;
    }

    private static String toStringId(Long id) {
        return id == null ? null : String.valueOf(id);
    }

    private String generateTaskNo() {
        String timePart = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        int randomPart = ThreadLocalRandom.current().nextInt(1000);
        return "HLK" + timePart + String.format("%03d", randomPart);
    }

    private String normalizeExtension(String ext) {
        if (!StringUtils.hasText(ext)) {
            return "";
        }
        return ext.toLowerCase(Locale.ROOT);
    }

    private void validateAudioExtension(String ext) {
        if (!ALLOWED_AUDIO_EXT.contains(ext)) {
            throw new ServiceException("仅支持 mp3、wav、m4a、aac 格式");
        }
    }

    private String resolveContentType(FileAsset asset) {
        if (StringUtils.hasText(asset.getMimeType())) {
            return asset.getMimeType();
        }
        String ext = asset.getFileExt();
        if (!StringUtils.hasText(ext)) {
            return MediaType.APPLICATION_OCTET_STREAM_VALUE;
        }
        return switch (ext.toLowerCase(Locale.ROOT)) {
            case "mp3" -> "audio/mpeg";
            case "wav" -> "audio/wav";
            case "m4a" -> "audio/mp4";
            case "aac" -> "audio/aac";
            case "jpg", "jpeg" -> "image/jpeg";
            case "png" -> "image/png";
            default -> MediaType.APPLICATION_OCTET_STREAM_VALUE;
        };
    }
}
