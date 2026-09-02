package com.zhs.exception;

import com.zhs.common.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Locale;

/**
 * 全局异常处理器
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * 业务异常（自定义 code + message）
     */
    @ExceptionHandler(ServiceException.class)
    public Result<?> handleServiceException(ServiceException e) {
        Integer code = e.getCode();
        return code != null ? Result.fail(code, e.getMessage()) : Result.fail(e.getMessage());
    }

    /**
     * 唯一键冲突：数据库约束名只写日志，不把 SQL 和堆栈展示给用户。
     */
    @ExceptionHandler(DuplicateKeyException.class)
    public Result<?> handleDuplicateKey(DuplicateKeyException e) {
        String detail = mostSpecificMessage(e);
        log.warn("保存数据时发生唯一键冲突: {}", detail);
        return Result.fail(409, duplicateMessage(detail));
    }

    /**
     * 外键、非空等数据完整性约束。
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public Result<?> handleDataIntegrity(DataIntegrityViolationException e) {
        String detail = mostSpecificMessage(e);
        log.warn("保存数据时违反数据库约束: {}", detail);
        String normalized = detail.toLowerCase(Locale.ROOT);
        if (normalized.contains("foreign key constraint fails")) {
            if (normalized.contains("cannot delete") || normalized.contains("cannot update")) {
                return Result.fail(409, "该数据正在被其他内容使用，暂时不能删除");
            }
            return Result.fail(409, "关联的数据不存在或已失效，请重新选择");
        }
        if (normalized.contains("cannot be null")) {
            return Result.fail(400, "必填信息不完整，请检查后再保存");
        }
        return Result.fail(409, "数据状态冲突，请检查填写内容后重试");
    }

    /**
     * 未知异常
     */
    @ExceptionHandler(Exception.class)
    public Result<?> handleException(Exception e) {
        log.error("未处理的服务器异常", e);
        return Result.fail("服务器开小差了，请稍后重试");
    }

    private String mostSpecificMessage(Throwable e) {
        Throwable current = e;
        while (current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        return current.getMessage() == null ? "" : current.getMessage();
    }

    private String duplicateMessage(String detail) {
        String normalized = detail.toLowerCase(Locale.ROOT);
        if (normalized.contains("uk_hearing_challenge_code")) {
            return "题目编码已存在，请更换后再保存";
        }
        if (normalized.contains("uk_challenge_audio_code")) {
            return "音频编码已存在，请更换后再保存";
        }
        if (normalized.contains("uk_scenario_code")) {
            return "场景编码已存在，请更换后再保存";
        }
        if (normalized.contains("uk_sample_code")) {
            return "示例音编码已存在，请更换后再保存";
        }
        if (normalized.contains("uk_content_category_code")) {
            return "分类编码已存在，请更换后再保存";
        }
        if (normalized.contains("uk_content_slug_lang")) {
            return "同一语言下的文章地址已存在，请更换";
        }
        if (normalized.contains("uk_read_category_code")) {
            return "点读分类编码已存在，请更换";
        }
        if (normalized.contains("uk_read_item_code")) {
            return "点读内容编码已存在，请更换";
        }
        if (normalized.contains("uk_system_config_key")) {
            return "配置键已存在，请勿重复添加";
        }
        if (normalized.contains("uk_file_storage_object")) {
            return "该文件已经上传，无需重复添加";
        }
        if (normalized.contains("uk_sensitive_open_id") || normalized.contains("uk_sensitive_user_id")) {
            return "该微信账号已经绑定";
        }
        return "相同数据已经存在，请勿重复添加";
    }
}
