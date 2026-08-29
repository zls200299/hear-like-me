package com.zhs.controller.api;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.zhs.common.NoLoginRequest;
import com.zhs.exception.ServiceException;
import com.zhs.model.AudioProcessingTask;
import com.zhs.model.FileAsset;
import com.zhs.model.SampleAudio;
import com.zhs.model.ScenarioPreset;
import com.zhs.request.api.AudioTaskCreateReq;
import com.zhs.service.IAudioProcessingTaskService;
import com.zhs.service.IFileAssetService;
import com.zhs.service.ISampleAudioService;
import com.zhs.service.IScenarioPresetService;
import com.zhs.util.R;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

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

    private static final String AUDIO_BASE_DIR = "D:/hear-like-me/data/audio";

    @Resource
    private IScenarioPresetService scenarioPresetService;

    @Resource
    private ISampleAudioService sampleAudioService;

    @Resource
    private IFileAssetService fileAssetService;

    @Resource
    private IAudioProcessingTaskService audioProcessingTaskService;

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
        if (file.isEmpty()) {
            throw new ServiceException("文件不能为空");
        }

        try {
            // 生成日期路径
            String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
            String dirPath = AUDIO_BASE_DIR + "/input/" + datePath;
            File dir = new File(dirPath);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            // 原始文件名 & 扩展名
            String originalFilename = file.getOriginalFilename();
            String ext = StringUtils.getFilenameExtension(originalFilename);
            if (ext == null) {
                ext = "wav";
            }

            // UUID 文件名
            String uuid = UUID.randomUUID().toString().replace("-", "");
            String savedFilename = uuid + "." + ext;
            String fullPath = dirPath + "/" + savedFilename;

            // 写盘
            file.transferTo(new File(fullPath));

            // 写入 file_asset 表
            FileAsset asset = new FileAsset();
            asset.setOwnerUserId(null);
            asset.setAssetType("AUDIO_SOURCE");
            asset.setStorageProvider("LOCAL");
            asset.setBucketName("");
            asset.setObjectKey("input/" + datePath + "/" + savedFilename);
            asset.setOriginalFilename(originalFilename);
            asset.setFileExt(ext);
            asset.setMimeType(file.getContentType());
            asset.setFileSize(file.getSize());
            asset.setAccessMode("PRIVATE");
            asset.setStatus("ACTIVE");
            fileAssetService.save(asset);

            // 返回
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("assetId", asset.getId());
            result.put("accessUrl", "/files/audio/" + asset.getObjectKey());
            return R.ok(result);

        } catch (IOException e) {
            log.error("文件保存失败", e);
            throw new ServiceException("文件保存失败: " + e.getMessage());
        }
    }

    // ==================== 4. 创建音频处理任务 ====================

    @PostMapping("/audio/tasks")
    @Transactional
    public R<Map<String, String>> createTask(@RequestBody AudioTaskCreateReq req) {
        // 校验 sourceAssetId
        if (req.getSourceAssetId() == null) {
            throw new ServiceException("sourceAssetId 不能为空");
        }

        // 查询源文件
        FileAsset sourceAsset = fileAssetService.getById(req.getSourceAssetId());
        if (sourceAsset == null) {
            throw new ServiceException("源文件不存在");
        }

        // 生成 taskNo
        String taskNo = UUID.randomUUID().toString().replace("-", "");

        // 创建任务记录
        AudioProcessingTask task = new AudioProcessingTask();
        task.setTaskNo(taskNo);
        task.setUserId(null);
        task.setSourceType(req.getSourceType() != null ? req.getSourceType() : "UPLOAD");
        task.setSourceAssetId(req.getSourceAssetId());
        task.setScenarioCode(req.getScenarioCode());
        task.setTaskStatus("SUCCESS");
        task.setProgress(100);
        task.setRetryCount(0);
        audioProcessingTaskService.save(task);

        // 复制源文件到 output 目录
        String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        String outputDirPath = AUDIO_BASE_DIR + "/output/" + datePath;
        File outputDir = new File(outputDirPath);
        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }

        String sourceFullPath = AUDIO_BASE_DIR + "/" + sourceAsset.getObjectKey();
        String outputFilename = taskNo + "." + (sourceAsset.getFileExt() != null ? sourceAsset.getFileExt() : "wav");
        String outputFullPath = outputDirPath + "/" + outputFilename;

        try {
            Files.copy(Path.of(sourceFullPath), Path.of(outputFullPath));
        } catch (IOException e) {
            log.error("文件复制失败: {} -> {}", sourceFullPath, outputFullPath, e);
            throw new ServiceException("文件复制失败: " + e.getMessage());
        }

        // 写入 output file_asset
        FileAsset outputAsset = new FileAsset();
        outputAsset.setOwnerUserId(null);
        outputAsset.setParentAssetId(sourceAsset.getId());
        outputAsset.setAssetType("AUDIO_OUTPUT");
        outputAsset.setStorageProvider("LOCAL");
        outputAsset.setBucketName("");
        outputAsset.setObjectKey("output/" + datePath + "/" + outputFilename);
        outputAsset.setOriginalFilename(sourceAsset.getOriginalFilename());
        outputAsset.setFileExt(sourceAsset.getFileExt());
        outputAsset.setMimeType(sourceAsset.getMimeType());
        outputAsset.setFileSize(sourceAsset.getFileSize());
        outputAsset.setAccessMode("PRIVATE");
        outputAsset.setStatus("ACTIVE");
        fileAssetService.save(outputAsset);

        // 回填 outputAssetId
        task.setOutputAssetId(outputAsset.getId());
        audioProcessingTaskService.updateById(task);

        // 返回 taskNo
        Map<String, String> result = new LinkedHashMap<>();
        result.put("taskNo", taskNo);
        return R.ok(result);
    }

    // ==================== 5. 查询任务详情 ====================

    @GetMapping("/audio/tasks/{taskNo}")
    public R<Map<String, Object>> getTaskDetail(@PathVariable String taskNo) {
        LambdaQueryWrapper<AudioProcessingTask> wrapper = new QueryWrapper<AudioProcessingTask>().lambda()
                .eq(AudioProcessingTask::getTaskNo, taskNo)
                .eq(AudioProcessingTask::getIsDelete, 0);
        AudioProcessingTask task = audioProcessingTaskService.getOne(wrapper);
        if (task == null) {
            throw new ServiceException("任务不存在");
        }

        // 拼 outputUrl
        String outputUrl = null;
        if (task.getOutputAssetId() != null) {
            FileAsset outputAsset = fileAssetService.getById(task.getOutputAssetId());
            if (outputAsset != null) {
                outputUrl = "/files/audio/" + outputAsset.getObjectKey();
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("status", task.getTaskStatus());
        result.put("outputUrl", outputUrl);
        result.put("clarityScore", task.getClarityScore());
        result.put("clarityGrade", task.getClarityGrade());
        result.put("errorMessage", task.getErrorMessage());
        return R.ok(result);
    }
}