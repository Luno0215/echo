package com.luno.echo.common;

import lombok.Data;
import java.io.Serializable;

/**
 * 统一返回结果类
 * 作用：后端给前端的“统一快递盒”，所有接口必须返回这个类型。
 * @param <T> 数据泛型（可能是 User 对象，也可能是 List 集合）
 */
@Data
public class Result<T> implements Serializable {

    private int code;        // 状态码 (200=成功, 40001=参数错...)
    private T data;          // 数据内容 (成功时有值，失败时为null)
    private String message;  // 提示消息 ("操作成功" 或 "账号已存在")
    private String description; // 详细描述 (用于排查问题，可选)

    /**
     * 构造函数 (私有化，强制使用静态方法创建)
     */
    public Result(int code, T data, String message, String description) {
        this.code = code;
        this.data = data;
        this.message = message;
        this.description = description;
    }

    // ============================ 成功响应 ============================

    /**
     * 成功 (带数据)
     * 场景：查询用户信息、登录成功
     */
    public static <T> Result<T> ok(T data) {
        return new Result<>(0, data, "ok", "");
    }

    /**
     * 成功 (无数据)
     * 场景：修改密码成功、删除成功
     */
    public static <T> Result<T> ok() {
        return new Result<>(0, null, "ok", "");
    }

    // ============================ 失败响应  ============================

    /**
     * 失败 (使用 ErrorCode 枚举) - 最常用
     * 场景：校验失败、逻辑错误
     */
    public static <T> Result<T> error(ErrorCode errorCode) {
        return new Result<>(errorCode.getCode(), null, errorCode.getMessage(), errorCode.getDescription());
    }

    /**
     * 失败 (使用 ErrorCode，但覆盖消息)
     * 场景：参数错误，但想告诉前端具体是哪个参数错了
     * 例如：Result.error(ErrorCode.PARAMS_ERROR, "账号长度不能小于4位")
     */
    public static <T> Result<T> error(ErrorCode errorCode, String message) {
        return new Result<>(errorCode.getCode(), null, message, errorCode.getDescription());
    }

    /**
     * 失败 (自定义 Code 和 Message)
     * 场景：全局异常处理器中，透传异常里的 code
     */
    public static <T> Result<T> error(int code, String message, String description) {
        return new Result<>(code, null, message, description);
    }
}