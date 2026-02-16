package com.luno.echo.model.dto;

import lombok.Data;
import java.io.Serializable;

@Data
public class PostDeleteRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    // 删除时只需要传帖子 ID
    private Long id;
}