package com.zhs.service.engine;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.zhs.config.EngineProperties;
import com.zhs.exception.ServiceException;
import com.zhs.model.FileAsset;
import com.zhs.model.SampleAudio;
import com.zhs.service.IFileAssetService;
import com.zhs.service.ISampleAudioService;
import com.zhs.service.storage.LocalFileStorageService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 生成内置示例原声音频并保存为 AUDIO_SOURCE
 */
@Slf4j
@Service
public class SampleSourceService {

    private static final String DEFAULT_OWNER = "guest";
    private static final Set<String> ALLOWED_SAMPLE_CODES = Set.of("vowel", "tone", "melody");

    private static final Map<String, String> SAMPLE_FILE_NAMES = Map.of(
            "vowel", "元音示例.wav",
            "tone", "声调示例.wav",
            "melody", "旋律示例.wav"
    );

    @Resource
    private EngineProperties engineProperties;

    @Resource
    private ProcessCommandExecutor processCommandExecutor;

    @Resource
    private LocalFileStorageService localFileStorageService;

    @Resource
    private IFileAssetService fileAssetService;

    @Resource
    private ISampleAudioService sampleAudioService;

    public Map<String, Object> prepareSampleSource(String sampleCode) {
        String normalizedCode = normalizeSampleCode(sampleCode);
        validateSampleEnabled(normalizedCode);

        String objectKey = localFileStorageService.buildAudioSampleObjectKey(DEFAULT_OWNER, normalizedCode);
        localFileStorageService.createParentDirectories(objectKey);
        Path outputPath = localFileStorageService.resolvePath(objectKey);

        log.info("准备示例原声 sampleCode={}, objectKey={}", normalizedCode, objectKey);

        Path scriptPath = engineProperties.resolveScriptPath();
        if (!Files.exists(scriptPath)) {
            throw new ServiceException("声码器脚本不存在: " + scriptPath);
        }

        List<String> command = new ArrayList<>();
        command.add(engineProperties.getPythonPath());
        command.add(scriptPath.toString());
        command.add("--sample");
        command.add(normalizedCode);
        command.add("--output");
        command.add(outputPath.toString());
        command.add("--original-only");

        processCommandExecutor.execute("示例原声生成", command);

        if (!Files.exists(outputPath) || fileSize(outputPath) <= 0) {
            throw new ServiceException("示例原声文件无效");
        }

        String fileName = SAMPLE_FILE_NAMES.getOrDefault(normalizedCode, normalizedCode + ".wav");

        FileAsset asset = new FileAsset();
        asset.setOwnerUserId(null);
        asset.setAssetType("AUDIO_SOURCE");
        asset.setStorageProvider("LOCAL");
        asset.setBucketName("");
        asset.setObjectKey(objectKey);
        asset.setOriginalFilename(fileName);
        asset.setFileExt("wav");
        asset.setMimeType("audio/wav");
        asset.setFileSize(fileSize(outputPath));
        asset.setAccessMode("PRIVATE");
        asset.setStatus("ACTIVE");
        fileAssetService.save(asset);

        log.info("示例原声已保存 assetId={}, sampleCode={}, objectKey={}",
                asset.getId(), normalizedCode, objectKey);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("assetId", String.valueOf(asset.getId()));
        result.put("sampleCode", normalizedCode);
        result.put("fileName", fileName);
        result.put("url", localFileStorageService.buildPreviewUrl(asset.getId()));
        result.put("objectKey", objectKey);
        return result;
    }

    private void validateSampleEnabled(String sampleCode) {
        LambdaQueryWrapper<SampleAudio> wrapper = new QueryWrapper<SampleAudio>().lambda()
                .eq(SampleAudio::getSampleCode, sampleCode)
                .eq(SampleAudio::getEnabled, 1)
                .eq(SampleAudio::getIsDelete, 0);
        SampleAudio sampleAudio = sampleAudioService.getOne(wrapper);
        if (sampleAudio == null) {
            throw new ServiceException("示例声音不可用: " + sampleCode);
        }
    }

    private static String normalizeSampleCode(String sampleCode) {
        if (!StringUtils.hasText(sampleCode)) {
            throw new ServiceException("sampleCode 不能为空");
        }
        String code = sampleCode.trim().toLowerCase();
        if (!ALLOWED_SAMPLE_CODES.contains(code)) {
            throw new ServiceException("不支持的示例声音: " + sampleCode);
        }
        return code;
    }

    private static long fileSize(Path path) {
        try {
            return Files.size(path);
        } catch (Exception e) {
            return 0L;
        }
    }
}
