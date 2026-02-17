package com.luno.echo.common.exception;

import com.luno.echo.common.ErrorCode;
import com.luno.echo.common.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器
 * 作用：捕获 Controller 和 Service 层抛出的所有异常，统一转为 Result 对象返回。
 * 它是项目的“安全网”。
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * 1. 捕获【自定义业务异常】
     * 场景：账号已存在、密码错误、权限不足
     * 逻辑：这是预期的错误，我们要把具体的 code 和 message 返回给前端。
     */
    @ExceptionHandler(BusinessException.class)
    public Result<?> businessExceptionHandler(BusinessException e) {
        log.info("businessException: " + e.getMessage(), e);
        return Result.error(e.getCode(), e.getMessage(), e.getDescription());
    }

    /**
     * 2. 捕获【系统运行时异常】
     * 场景：空指针 (NPE)、数组越界、数据库连接失败
     * 逻辑：这是未预期的 Bug，为了安全，统一告诉前端“系统内部异常”，不要暴露代码细节。
     */
    @ExceptionHandler(RuntimeException.class)
    public Result<?> runtimeExceptionHandler(RuntimeException e) {
        log.error("runtimeException", e);
        // 统一返回 50000 错误码
        return Result.error(ErrorCode.SYSTEM_ERROR.getCode(), "系统内部异常", "");
    }

    /**
     * 捕获参数校验异常 (JSR-303)
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<?> handleBindException(MethodArgumentNotValidException e) {
        // 获取所有校验失败的字段和错误信息
        BindingResult bindingResult = e.getBindingResult();
        StringBuilder sb = new StringBuilder();

        bindingResult.getFieldErrors().forEach(error -> {
            sb.append(error.getField()).append(": ").append(error.getDefaultMessage()).append("; ");
        });

        // 返回 Params_ERROR，并带上具体的错误信息
        return Result.error(ErrorCode.PARAMS_ERROR, sb.toString());
    }
}