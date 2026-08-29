package com.zhs.service.storage;

import com.zhs.config.StorageProperties;
import com.zhs.exception.ServiceException;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * 本地文件存储服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LocalFileStorageService {

    private static final DateTimeFormatter DATE_PATH_FORMATTER = DateTimeFormatter.ofPattern("yyyy/MM/dd");

    private final StorageProperties storageProperties;

    private Path rootPath;

    @PostConstruct
    public void init() {
        if (!StringUtils.hasText(storageProperties.getRootPath())) {
            throw new IllegalStateException("hear-like-me.storage.root-path 未配置");
        }
        rootPath = Paths.get(storageProperties.getRootPath()).toAbsolutePath().normalize();
        try {
            Files.createDirectories(rootPath);
            log.info("本地文件存储根目录: {}", rootPath);
        } catch (IOException e) {
            throw new IllegalStateException("无法创建本地存储目录: " + rootPath, e);
        }
    }

    public Path resolvePath(String objectKey) {
        String normalizedKey = normalizeObjectKey(objectKey);
        Path resolved = rootPath.resolve(normalizedKey).normalize();
        if (!resolved.startsWith(rootPath)) {
            throw new ServiceException("非法文件路径");
        }
        return resolved;
    }

    public void createParentDirectories(String objectKey) {
        Path targetPath = resolvePath(objectKey);
        try {
            Files.createDirectories(targetPath.getParent());
        } catch (IOException e) {
            throw new ServiceException("创建目录失败: " + e.getMessage());
        }
    }

    public void saveMultipartFile(MultipartFile file, String objectKey) {
        createParentDirectories(objectKey);
        Path targetPath = resolvePath(objectKey);
        try {
            file.transferTo(targetPath);
        } catch (IOException e) {
            throw new ServiceException("文件保存失败: " + e.getMessage());
        }
    }

    public InputStream readFile(String objectKey) {
        Path targetPath = resolvePath(objectKey);
        if (!Files.exists(targetPath)) {
            throw new ServiceException("文件不存在");
        }
        try {
            return Files.newInputStream(targetPath);
        } catch (IOException e) {
            throw new ServiceException("文件读取失败: " + e.getMessage());
        }
    }

    public boolean exists(String objectKey) {
        return Files.exists(resolvePath(objectKey));
    }

    public void copy(String sourceObjectKey, String targetObjectKey) {
        Path sourcePath = resolvePath(sourceObjectKey);
        if (!Files.exists(sourcePath)) {
            throw new ServiceException("源文件不存在");
        }
        createParentDirectories(targetObjectKey);
        Path targetPath = resolvePath(targetObjectKey);
        try {
            Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new ServiceException("文件复制失败: " + e.getMessage());
        }
    }

    public String buildPreviewUrl(Long assetId) {
        String prefix = normalizePreviewPrefix();
        return prefix + "/api/files/preview/" + assetId;
    }

    public String buildAudioInputObjectKey(String owner, String ext) {
        return buildObjectKey("audio/input", owner, ext);
    }

    public String buildAudioOutputObjectKey(String owner, String filename) {
        String datePath = LocalDate.now().format(DATE_PATH_FORMATTER);
        String safeOwner = sanitizeOwner(owner);
        return "audio/output/" + datePath + "/" + safeOwner + "/" + filename;
    }

    public String buildAudioNormalizedObjectKey(String owner, String ext) {
        return buildObjectKey("audio/normalized", owner, ext);
    }

    public String buildAudioSampleObjectKey(String owner, String sampleCode) {
        String datePath = LocalDate.now().format(DATE_PATH_FORMATTER);
        String safeOwner = sanitizeOwner(owner);
        String safeCode = sanitizeOwner(sampleCode);
        String filename = safeCode + "_" + UUID.randomUUID().toString().replace("-", "") + ".wav";
        return "audio/sample/" + datePath + "/" + safeOwner + "/" + filename;
    }

    private String buildObjectKey(String categoryPrefix, String owner, String ext) {
        String datePath = LocalDate.now().format(DATE_PATH_FORMATTER);
        String safeOwner = sanitizeOwner(owner);
        String filename = UUID.randomUUID().toString().replace("-", "") + "." + ext.toLowerCase();
        return categoryPrefix + "/" + datePath + "/" + safeOwner + "/" + filename;
    }

    private String sanitizeOwner(String owner) {
        if (!StringUtils.hasText(owner)) {
            return "guest";
        }
        return owner.replaceAll("[^a-zA-Z0-9_-]", "");
    }

    private String normalizeObjectKey(String objectKey) {
        if (!StringUtils.hasText(objectKey)) {
            throw new ServiceException("objectKey 不能为空");
        }
        return objectKey.replace("\\", "/").replaceAll("^/+", "");
    }

    private String normalizePreviewPrefix() {
        String prefix = storageProperties.getPreviewUrlPrefix();
        if (!StringUtils.hasText(prefix)) {
            throw new ServiceException("preview-url-prefix 未配置");
        }
        return prefix.endsWith("/") ? prefix.substring(0, prefix.length() - 1) : prefix;
    }
}
