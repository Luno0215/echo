package com.luno.echo.model.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 用户信息更新请求体
 */
@Data
public class UserUpdateRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 用户昵称 (可选)
     */
    private String nickname;

    /**
     * 用户头像 URL (可选)
     */
    private String avatar;
}