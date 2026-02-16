package com.luno.echo.common.exception;


import com.luno.echo.common.ErrorCode;

/**
 * 自定义业务异常
 * 作用：在 Service 层发现逻辑错误时，直接 throw 出去。
 * 特点：继承 RuntimeException，不需要在该方法后写 throws，代码更干净。
 */
public class BusinessException extends RuntimeException {

    /**
     * 异常对应的错误码 (这是重点，要透传给 Result)
     */
    private final int code;

    /**
     * 详细描述
     */
    private final String description;

    /**
     * 方式 1: 直接传错误码枚举 (最常用)
     * 用法: throw new BusinessException(ErrorCode.PARAMS_ERROR);
     */
    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
        this.description = errorCode.getDescription();
    }

    /**
     * 方式 2: 传枚举 + 自定义消息 (用于参数校验)
     * 用法: throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号太短");
     */
    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.code = errorCode.getCode();
        this.description = errorCode.getDescription();
    }

    /**
     * 方式 3: 传枚举 + 消息 + 描述 (最全)
     */
    public BusinessException(ErrorCode errorCode, String message, String description) {
        super(message);
        this.code = errorCode.getCode();
        this.description = description;
    }

    public int getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }
}