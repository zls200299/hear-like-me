package com.zhs.exception;

import com.zhs.common.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器
 */
@RestControllerAdvice
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
     * 未知异常
     */
    @ExceptionHandler(Exception.class)
    public Result<?> handleException(Exception e) {
        return Result.fail("服务器内部错误：" + e.getMessage());
    }
}
