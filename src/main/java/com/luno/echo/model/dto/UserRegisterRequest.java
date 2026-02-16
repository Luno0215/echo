package com.luno.echo.model.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 用户注册请求体
 * 作用：专门用于接收前端注册接口传递的 JSON 参数
 */
@Data
public class UserRegisterRequest implements Serializable {

    private static final long serialVersionUID = 3191241716373120793L;

    /**
     * 用户账号
     */
    private String username;

    /**
     * 用户密码
     */
    private String password;

    /**
     * 校验密码（确认密码）
     */
    private String checkPassword;
    
}