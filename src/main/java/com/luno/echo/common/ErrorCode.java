package com.luno.echo.common;

/**
 * 全局错误码枚举
 * 作用：统一定义项目中的“错误字典”，避免魔法数字。
 */
public enum ErrorCode {

    // --- 0: 成功 ---
    SUCCESS(0, "ok", ""),

    // --- 4xxxx: 客户端/业务逻辑错误 ---
    PARAMS_ERROR(40000, "请求参数错误", ""),
    NULL_ERROR(40001, "请求数据为空", ""),
    NOT_LOGIN(40100, "未登录", ""),
    NO_AUTH(40101, "无权限", ""),

    // --- 5xxxx: 服务端内部错误 ---
    SYSTEM_ERROR(50000, "系统内部异常", "");

    /**
     * 状态码
     */
    private final int code;

    /**
     * 状态码信息 (给用户看)
     */
    private final String message;

    /**
     * 状态码描述 (给开发看)
     */
    private final String description;

    ErrorCode(int code, String message, String description) {
        this.code = code;
        this.message = message;
        this.description = description;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public String getDescription() {
        return description;
    }
}