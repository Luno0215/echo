package com.luno.echo.model.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class PostAddRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    // 前端只用传内容和标签，userId 后端自己从 UserHolder 拿，这样最安全
    private String content;
    private String tag;
}