package com.zhs.service.engine;

import com.zhs.config.EngineProperties;
import com.zhs.exception.ServiceException;
import com.zhs.model.FileAsset;
import com.zhs.service.IFileAssetService;
import com.zhs.service.storage.LocalFileStorageService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * 使用 FFmpeg 将输入音频标准化为 44.1kHz 单声道 16-bit PCM WAV
 */
@Slf4j
@Service
public class FFmpegNormalizeService {

    @Resource
    private EngineProperties engineProperties;

    @Resource
    private ProcessCommandExecutor processCommandExecutor;

    @Resource
    private LocalFileStorageService localFileStorageService;

    @Resource
    private IFileAssetService fileAssetService;

    public NormalizeResult normalize(Path sourceFilePath, FileAsset sourceAsset, String owner) {
        if (sourceFilePath == null || !Files.exists(sourceFilePath)) {
            throw new ServiceException("源文件不存在");
        }

        String objectKey = localFileStorageService.buildAudioNormalizedObjectKey(owner, "wav");
        localFileStorageService.createParentDirectories(objectKey);
        Path outputPath = localFileStorageService.resolvePath(objectKey);

        log.info("FFmpeg 输入文件: {}", sourceFilePath);
        log.info("FFmpeg normalized objectKey: {}", objectKey);

        List<String> command = buildCommand(sourceFilePath, outputPath);
        ProcessCommandExecutor.ProcessResult processResult =
                processCommandExecutor.execute("FFmpeg 标准化", command);

        log.info("FFmpeg 完成, exitCode={}, 耗时={}ms, normalizedObjectKey={}",
                processResult.exitCode(), processResult.durationMs(), objectKey);

        if (!Files.exists(outputPath) || fileSize(outputPath) <= 0) {
            throw new ServiceException("FFmpeg 输出文件无效");
        }

        FileAsset asset = new FileAsset();
        asset.setOwnerUserId(null);
        asset.setParentAssetId(sourceAsset.getId());
        asset.setAssetType("AUDIO_NORMALIZED");
        asset.setStorageProvider("LOCAL");
        asset.setBucketName("");
        asset.setObjectKey(objectKey);
        asset.setOriginalFilename(resolveNormalizedFilename(sourceAsset.getOriginalFilename()));
        asset.setFileExt("wav");
        asset.setMimeType("audio/wav");
        asset.setFileSize(fileSize(outputPath));
        asset.setAccessMode("PRIVATE");
        asset.setStatus("ACTIVE");
        fileAssetService.save(asset);

        log.info("FFmpeg normalized file_asset id={}, size={}", asset.getId(), asset.getFileSize());

        NormalizeResult result = new NormalizeResult();
        result.setAsset(asset);
        result.setLocalPath(outputPath);
        return result;
    }

    private List<String> buildCommand(Path sourceFilePath, Path outputPath) {
        List<String> command = new ArrayList<>();
        command.add(engineProperties.getFfmpegPath());
        command.add("-y");
        command.add("-i");
        command.add(sourceFilePath.toString());
        command.add("-ac");
        command.add("1");
        command.add("-ar");
        command.add("44100");
        command.add("-sample_fmt");
        command.add("s16");
        command.add(outputPath.toString());
        return command;
    }

    private static long fileSize(Path path) {
        try {
            return Files.size(path);
        } catch (Exception e) {
            return 0L;
        }
    }

    private static String resolveNormalizedFilename(String originalFilename) {
        if (!StringUtils.hasText(originalFilename)) {
            return "normalized.wav";
        }
        int dot = originalFilename.lastIndexOf('.');
        String base = dot > 0 ? originalFilename.substring(0, dot) : originalFilename;
        return base + ".wav";
    }
}
