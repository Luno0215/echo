package com.luno.echo.model.dto;

import lombok.Data;
import java.io.Serializable;

@Data
public class CommentAddRequest implements Serializable {
    private Long postId; // 给哪个帖子评论
    private String content; // 评论内容
}