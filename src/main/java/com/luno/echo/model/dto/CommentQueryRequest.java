package com.luno.echo.model.dto;

import lombok.Data;
import java.io.Serializable;

@Data
public class CommentQueryRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 必须传：查看哪个帖子的评论
     */
    private Long postId;

    /**
     * 分页参数
     */
    private long current = 1;
    private long pageSize = 10;
}