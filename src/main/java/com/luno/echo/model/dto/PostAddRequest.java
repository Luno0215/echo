package com.luno.echo.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

@Data
public class PostAddRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    // 前端只用传内容和标签，userId 后端自己从 UserHolder 拿，这样最安全

    // @NotBlank: 只能用于 String，不能为 null，且 trim() 之后长度 > 0
    @NotBlank(message = "内容不能为空valid")
    @Size(max = 500, message = "内容过长，不能超过500字")
    private String content;

    @Size(max = 20, message = "标签最长20个字符valid")
    private String tag;
}